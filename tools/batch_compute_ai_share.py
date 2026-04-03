
#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import difflib
import subprocess
from collections import Counter, defaultdict
from pathlib import Path

from common import (
    load_csv_rows,
    normalize_code_line,
    normalize_file_path,
    parse_bool,
    parse_float,
    sha256_short,
    write_csv_rows,
)

DIFF_PREFIX_RENAME_OLD = "rename from "
DIFF_PREFIX_RENAME_NEW = "rename to "


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def is_noise_line(content: str) -> bool:
    stripped = content.strip()
    if not stripped:
        return True
    if stripped.startswith("import "):
        return True
    if stripped.startswith("package "):
        return True
    if stripped in {"{", "}", ");", "});"}:
        return True
    return False


def parse_unified_diff(diff_text: str) -> dict[str, list[str]]:
    file_lines: dict[str, list[str]] = defaultdict(list)
    current_file = ""
    for line in diff_text.splitlines():
        if line.startswith("+++ "):
            current_file = normalize_file_path(line[4:].strip())
            if current_file.startswith("b/"):
                current_file = current_file[2:]
            continue
        if line.startswith("--- ") or line.startswith("@@"):
            continue
        if line.startswith("+") and not line.startswith("+++"):
            content = normalize_code_line(line[1:])
            if is_noise_line(content):
                continue
            file_lines[current_file].append(content)
    return file_lines


def run_git_diff(repo_root: Path, base_ref: str, target_ref: str) -> str:
    cmd = [
        "git",
        "-C",
        str(repo_root),
        "diff",
        "--unified=0",
        "--no-color",
        f"{base_ref}..{target_ref}",
    ]
    completed = subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        check=False,
        encoding="utf-8",
        errors="replace",
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"git diff failed for {base_ref}..{target_ref}: {completed.stderr.strip()}"
        )
    return completed.stdout


def build_line_pool(file_map: dict[str, list[str]]) -> dict[str, Counter]:
    pool: dict[str, Counter] = {}
    for file_path, lines in file_map.items():
        pool[file_path] = Counter(lines)
    return pool


def fuzzy_match_one(final_line: str, ai_counter: Counter, threshold: float) -> bool:
    best_key = None
    best_score = 0.0
    for candidate, count in ai_counter.items():
        if count <= 0:
            continue
        score = difflib.SequenceMatcher(None, final_line, candidate).ratio()
        if score > best_score:
            best_score = score
            best_key = candidate
    if best_key is not None and best_score >= threshold:
        ai_counter[best_key] -= 1
        if ai_counter[best_key] <= 0:
            del ai_counter[best_key]
        return True
    return False


def compute_match(ai_map: dict[str, list[str]], final_map: dict[str, list[str]], threshold: float) -> tuple[int, int]:
    total_lines = sum(len(lines) for lines in final_map.values())
    matched = 0

    per_file_pool = build_line_pool(ai_map)
    global_pool = Counter()
    for lines in ai_map.values():
        global_pool.update(lines)

    for file_path, final_lines in final_map.items():
        local_pool = per_file_pool.setdefault(file_path, Counter())
        for final_line in final_lines:
            if local_pool.get(final_line, 0) > 0:
                local_pool[final_line] -= 1
                global_pool[final_line] -= 1
                if local_pool[final_line] <= 0:
                    del local_pool[final_line]
                if global_pool[final_line] <= 0:
                    del global_pool[final_line]
                matched += 1
                continue
            if fuzzy_match_one(final_line, local_pool, threshold):
                global_pool.subtract([final_line])
                matched += 1
                continue
            if fuzzy_match_one(final_line, global_pool, threshold):
                matched += 1

    return matched, total_lines


def compute_from_git(row: dict[str, str], repo_root: Path, threshold: float) -> dict[str, str]:
    scaffold_ref = row.get("scaffold_ref", "").strip()
    ai_snapshot_ref = row.get("ai_snapshot_ref", "").strip()
    final_ref = row.get("final_ref", "").strip()
    if not (scaffold_ref and final_ref):
        raise ValueError("scaffold_ref and final_ref are required for git mode")
    if row.get("condition") == "human-only":
        final_diff = run_git_diff(repo_root, scaffold_ref, final_ref)
        final_map = parse_unified_diff(final_diff)
        total_lines = sum(len(lines) for lines in final_map.values())
        return {
            "method": "git-human-only",
            "status": "ok",
            "ai_share": "0.0",
            "ai_lines": "0",
            "total_added_lines": str(total_lines),
            "note": "",
        }
    if not ai_snapshot_ref:
        raise ValueError("ai_snapshot_ref is required for AI-assisted runs in git mode")
    ai_diff = run_git_diff(repo_root, scaffold_ref, ai_snapshot_ref)
    final_diff = run_git_diff(repo_root, scaffold_ref, final_ref)
    ai_map = parse_unified_diff(ai_diff)
    final_map = parse_unified_diff(final_diff)
    matched, total_lines = compute_match(ai_map, final_map, threshold)
    share = matched / total_lines if total_lines else 0.0
    return {
        "method": "git",
        "status": "ok",
        "ai_share": f"{share:.4f}",
        "ai_lines": str(matched),
        "total_added_lines": str(total_lines),
        "note": "",
    }


def compute_from_files(row: dict[str, str], threshold: float) -> dict[str, str]:
    final_diff_path = Path(row.get("final_diff_path", ""))
    if not final_diff_path.exists():
        raise ValueError(f"missing final diff: {final_diff_path}")
    final_map = parse_unified_diff(read_text(final_diff_path))
    total_lines = sum(len(lines) for lines in final_map.values())
    if row.get("condition") == "human-only":
        return {
            "method": "file-human-only",
            "status": "ok",
            "ai_share": "0.0",
            "ai_lines": "0",
            "total_added_lines": str(total_lines),
            "note": "",
        }

    ai_snapshot_path = Path(row.get("ai_snapshot_diff_path", ""))
    if not ai_snapshot_path.exists():
        raise ValueError(f"missing ai snapshot diff: {ai_snapshot_path}")
    ai_map = parse_unified_diff(read_text(ai_snapshot_path))
    matched, total_lines = compute_match(ai_map, final_map, threshold)
    share = matched / total_lines if total_lines else 0.0
    return {
        "method": "file",
        "status": "ok",
        "ai_share": f"{share:.4f}",
        "ai_lines": str(matched),
        "total_added_lines": str(total_lines),
        "note": "",
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Batch-compute AI share for thesis runs using git refs or diff files."
    )
    parser.add_argument(
        "--manifest",
        required=True,
        help="Path to runs.csv manifest.",
    )
    parser.add_argument(
        "--output",
        default="data/ai_share.csv",
        help="Path to write the AI share results CSV.",
    )
    parser.add_argument(
        "--repo-root",
        help="Local git repository root. If provided and commit refs are present, git mode is used.",
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=0.85,
        help="Similarity threshold for fuzzy fallback matching. Exact file-aware matches are tried first.",
    )
    parser.add_argument(
        "--include-excluded",
        action="store_true",
        help="Process excluded runs too. By default, only include_in_analysis=true rows are processed.",
    )
    args = parser.parse_args()

    manifest_rows = load_csv_rows(Path(args.manifest))
    if not manifest_rows:
        raise SystemExit(f"No rows found in manifest: {args.manifest}")

    repo_root = Path(args.repo_root).expanduser().resolve() if args.repo_root else None
    output_rows: list[dict[str, str]] = []

    for row in manifest_rows:
        if not args.include_excluded and not parse_bool(row.get("include_in_analysis"), False):
            continue
        result = {
            "run_id": row.get("run_id", ""),
            "task_id": row.get("task_id", ""),
            "condition": row.get("condition", ""),
            "version": row.get("version", ""),
            "include_in_analysis": str(parse_bool(row.get("include_in_analysis"), False)).lower(),
            "ai_share": "",
            "ai_lines": "",
            "total_added_lines": "",
            "method": "",
            "status": "",
            "note": "",
        }
        try:
            if repo_root and row.get("scaffold_ref") and row.get("final_ref"):
                computed = compute_from_git(row, repo_root, args.threshold)
            else:
                computed = compute_from_files(row, args.threshold)
            result.update(computed)
        except Exception as exc:
            result.update(
                {
                    "method": "unavailable",
                    "status": "error",
                    "note": str(exc),
                }
            )
        output_rows.append(result)
        print(
            f"{result['run_id']}: status={result['status']}, ai_share={result['ai_share'] or 'NA'}, method={result['method']}"
        )

    fieldnames = [
        "run_id",
        "task_id",
        "condition",
        "version",
        "include_in_analysis",
        "ai_share",
        "ai_lines",
        "total_added_lines",
        "method",
        "status",
        "note",
    ]
    write_csv_rows(Path(args.output), fieldnames, output_rows)
    print(f"\nAI share results written to: {args.output}")


if __name__ == "__main__":
    main()
