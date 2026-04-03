
#!/usr/bin/env python3
from __future__ import annotations

import argparse
from collections import defaultdict
from pathlib import Path

from common import (
    detect_artifact_files,
    detect_diff_files,
    group_latest_versions,
    load_csv_rows,
    merge_existing_rows,
    parse_run_dir_name,
    sort_rows,
    write_csv_rows,
)

MANIFEST_FIELDS = [
    "run_id",
    "task_id",
    "task_number",
    "condition",
    "version",
    "include_in_analysis",
    "replacement_of",
    "results_dir",
    "artifacts_dir",
    "final_diff_path",
    "ai_snapshot_diff_path",
    "codeql_sarif",
    "semgrep_json",
    "semgrep_sarif",
    "spotbugs_xml",
    "gitleaks_sarif",
    "gitleaks_output",
    "depcheck_json",
    "tests_txt",
    "tests_xml",
    "pr_number",
    "scaffold_ref",
    "ai_snapshot_ref",
    "final_ref",
    "notes",
]


def discover_runs(results_root: Path) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for entry in sorted(results_root.iterdir()):
        if not entry.is_dir():
            continue
        parsed = parse_run_dir_name(entry.name)
        if not parsed:
            continue

        artifacts_dir = entry / "artifacts"
        row = {
            "run_id": parsed["run_id"],
            "task_id": parsed["task_id"],
            "task_number": str(parsed["task_number"]),
            "condition": parsed["condition"],
            "version": str(parsed["version"]),
            "include_in_analysis": "false",
            "replacement_of": "",
            "results_dir": str(entry),
            "artifacts_dir": str(artifacts_dir) if artifacts_dir.exists() else "",
            "final_diff_path": "",
            "ai_snapshot_diff_path": "",
            "codeql_sarif": "",
            "semgrep_json": "",
            "semgrep_sarif": "",
            "spotbugs_xml": "",
            "gitleaks_sarif": "",
            "gitleaks_output": "",
            "depcheck_json": "",
            "tests_txt": "",
            "tests_xml": "",
            "pr_number": "",
            "scaffold_ref": "",
            "ai_snapshot_ref": "",
            "final_ref": "",
            "notes": "",
        }
        row.update(detect_diff_files(entry))
        row.update(detect_artifact_files(artifacts_dir))
        rows.append(row)

    latest = group_latest_versions(rows)
    by_pair: dict[tuple[str, str], list[dict[str, str]]] = defaultdict(list)
    for row in rows:
        by_pair[(row["task_id"], row["condition"])].append(row)

    for row in rows:
        key = (row["task_id"], row["condition"])
        latest_row = latest[key]
        if row["run_id"] == latest_row["run_id"]:
            row["include_in_analysis"] = "true"
            if int(row["version"]) > 1:
                previous_versions = sorted(
                    [r for r in by_pair[key] if int(r["version"]) < int(row["version"])],
                    key=lambda item: int(item["version"]),
                )
                if previous_versions:
                    row["replacement_of"] = previous_versions[-1]["run_id"]
        else:
            row["include_in_analysis"] = "false"

    return sort_rows(rows)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Discover thesis result folders and build a manifest CSV."
    )
    parser.add_argument(
        "--results-root",
        required=True,
        help="Top-level directory that contains task-* result folders.",
    )
    parser.add_argument(
        "--output",
        default="data/runs.csv",
        help="Path to the manifest CSV to create or refresh.",
    )
    parser.add_argument(
        "--preserve-existing",
        action="store_true",
        help="If the output file already exists, preserve manually filled columns such as PR numbers and commit refs.",
    )
    args = parser.parse_args()

    results_root = Path(args.results_root).expanduser().resolve()
    output_path = Path(args.output).expanduser().resolve()

    if not results_root.exists():
        raise SystemExit(f"Results root does not exist: {results_root}")

    rows = discover_runs(results_root)

    if args.preserve_existing and output_path.exists():
        existing_rows = load_csv_rows(output_path)
        rows = merge_existing_rows(rows, existing_rows, key_field="run_id")
        rows = sort_rows(rows)

    write_csv_rows(output_path, MANIFEST_FIELDS, rows)

    included = sum(1 for row in rows if str(row["include_in_analysis"]).lower() == "true")
    excluded = len(rows) - included
    print(f"Manifest written to: {output_path}")
    print(f"Discovered runs:     {len(rows)}")
    print(f"Included by default: {included}")
    print(f"Excluded by default: {excluded}")
    print("\nReview and fill these columns before collecting GitHub metrics or Git-based AI share:")
    print("  - pr_number")
    print("  - scaffold_ref")
    print("  - ai_snapshot_ref")
    print("  - final_ref")


if __name__ == "__main__":
    main()
