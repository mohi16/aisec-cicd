
from __future__ import annotations

import csv
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

CONDITION_ORDER = ["human-only", "low-ai", "high-ai"]


def normalize_condition_slug(value: str) -> str:
    value = value.strip().lower().replace("_", "-")
    aliases = {
        "human": "human-only",
        "humanonly": "human-only",
        "human-only": "human-only",
        "lowai": "low-ai",
        "low-ai": "low-ai",
        "highai": "high-ai",
        "high-ai": "high-ai",
    }
    return aliases.get(value, value)


RUN_DIR_PATTERN = re.compile(
    r"^task-(?P<task_num>\d+)-(?P<condition>human-only|low-ai|high-ai)(?:-v(?P<version>\d+))?$",
    re.IGNORECASE,
)


def parse_run_dir_name(name: str) -> dict[str, Any] | None:
    match = RUN_DIR_PATTERN.match(name)
    if not match:
        return None
    task_num = int(match.group("task_num"))
    condition = normalize_condition_slug(match.group("condition"))
    version = int(match.group("version") or "1")
    return {
        "run_id": name,
        "task_id": f"T{task_num}",
        "task_number": task_num,
        "condition": condition,
        "version": version,
    }


def detect_artifact_files(artifacts_dir: Path) -> dict[str, str]:
    candidates = {
        "codeql_sarif": [
            "java.sarif",
            "codeql-results.sarif",
            "codeql.sarif",
        ],
        "semgrep_json": [
            "semgrep-results.json",
            "semgrep.json",
        ],
        "semgrep_sarif": [
            "semgrep-results.sarif",
            "semgrep.sarif",
        ],
        "spotbugs_xml": [
            "spotbugs.xml",
            "findsecbugs.xml",
        ],
        "gitleaks_sarif": [
            "gitleaks-results.sarif",
            "gitleaks-results.sarf",
            "gitleaks.sarif",
            "gitleaks.sarf",
        ],
        "gitleaks_output": [
            "gitleaks-output.txt",
            "gitleaks.txt",
        ],
        "depcheck_json": [
            "dependency-check-report.json",
            "dependency-check.json",
            "owasp-dependency-check-report.json",
        ],
        "tests_txt": [
            "com.thesis.securitystudy.SecurityStudyApplicationTests.txt",
        ],
        "tests_xml": [
            "TEST-com.thesis.securitystudy.SecurityStudyApplicationTests.xml",
            "TEST-com.thesis.securitystudy.SecurityStudyApplicationTests",
        ],
    }
    found: dict[str, str] = {}
    if not artifacts_dir.exists():
        return found

    lower_lookup = {p.name.lower(): p for p in artifacts_dir.iterdir() if p.is_file()}
    for key, names in candidates.items():
        chosen = None
        for name in names:
            chosen = lower_lookup.get(name.lower())
            if chosen:
                break
        if chosen:
            found[key] = str(chosen)

    # Flexible fallbacks
    if "codeql_sarif" not in found:
        for path in artifacts_dir.glob("*.sarif"):
            if "codeql" in path.name.lower() or path.name.lower() == "java.sarif":
                found["codeql_sarif"] = str(path)
                break

    if "spotbugs_xml" not in found:
        for path in artifacts_dir.glob("*.xml"):
            if "spotbugs" in path.name.lower() or "findsecbugs" in path.name.lower():
                found["spotbugs_xml"] = str(path)
                break

    return found


def detect_diff_files(run_dir: Path) -> dict[str, str]:
    candidates = {
        "final_diff_path": [
            "final.diff",
            "final.patch",
            "pr-final.diff",
        ],
        "ai_snapshot_diff_path": [
            "ai-snapshot.diff",
            "raw-ai.diff",
            "copilot.diff",
            "snapshot.diff",
            "ai.diff",
        ],
    }
    found: dict[str, str] = {}
    for key, names in candidates.items():
        for name in names:
            path = run_dir / name
            if path.exists():
                found[key] = str(path)
                break

    if "ai_snapshot_diff_path" not in found:
        for path in sorted(run_dir.glob("*snapshot*.diff")):
            found["ai_snapshot_diff_path"] = str(path)
            break

    return found


def load_csv_rows(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open("r", newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        return list(reader)


def write_csv_rows(path: Path, fieldnames: list[str], rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def sha256_short(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:16]


def normalize_whitespace(value: str) -> str:
    return re.sub(r"\s+", " ", value.strip())


def normalize_code_line(value: str) -> str:
    value = normalize_whitespace(value)
    value = value.rstrip(";")
    return value


def normalize_file_path(value: str) -> str:
    value = value.replace("\\", "/").strip()
    value = re.sub(r"^\./", "", value)
    while "//" in value:
        value = value.replace("//", "/")
    return value


def parse_bool(value: Any, default: bool = False) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    text = str(value).strip().lower()
    if text in {"1", "true", "yes", "y"}:
        return True
    if text in {"0", "false", "no", "n"}:
        return False
    return default


def parse_int(value: Any, default: int | None = None) -> int | None:
    try:
        if value is None or value == "":
            return default
        return int(float(str(value)))
    except (TypeError, ValueError):
        return default


def parse_float(value: Any, default: float | None = None) -> float | None:
    try:
        if value is None or value == "":
            return default
        return float(str(value))
    except (TypeError, ValueError):
        return default


def line_bucket(line: int, width: int = 3) -> int:
    if line <= 0:
        return 0
    return int((line - 1) / width) * width + 1


def group_latest_versions(rows: list[dict[str, Any]]) -> dict[tuple[str, str], dict[str, Any]]:
    latest: dict[tuple[str, str], dict[str, Any]] = {}
    for row in rows:
        key = (row["task_id"], row["condition"])
        current = latest.get(key)
        if current is None or int(row["version"]) > int(current["version"]):
            latest[key] = row
    return latest


def read_json(path: Path) -> dict[str, Any] | list[Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def merge_existing_rows(
    discovered_rows: list[dict[str, Any]],
    existing_rows: list[dict[str, str]],
    key_field: str = "run_id",
) -> list[dict[str, Any]]:
    existing_by_key = {row[key_field]: row for row in existing_rows if row.get(key_field)}
    merged: list[dict[str, Any]] = []
    for row in discovered_rows:
        existing = existing_by_key.get(str(row.get(key_field)))
        if existing:
            updated = dict(existing)
            updated.update({k: v for k, v in row.items() if v not in (None, "")})
            merged.append(updated)
        else:
            merged.append(row)
    return merged


def sort_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    def key(row: dict[str, Any]) -> tuple[int, str, int]:
        return (
            parse_int(row.get("task_number"), 999),
            str(row.get("condition", "")),
            parse_int(row.get("version"), 1),
        )
    return sorted(rows, key=key)


def summarize_missing(values: list[str]) -> str:
    counts: dict[str, int] = defaultdict(int)
    for value in values:
        counts[value] += 1
    return ", ".join(f"{k}: {v}" for k, v in sorted(counts.items()))
