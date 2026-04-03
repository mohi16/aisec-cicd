
#!/usr/bin/env python3
from __future__ import annotations

import argparse
from collections import defaultdict
from pathlib import Path
from typing import Any

from common import (
    load_csv_rows,
    normalize_file_path,
    parse_bool,
    parse_int,
    sha256_short,
    write_csv_rows,
)

FIELDS = [
    "vulnerability_id",
    "run_id",
    "pr_number",
    "task_id",
    "condition",
    "version",
    "include_in_analysis",
    "cwe_id",
    "severity",
    "file_path",
    "line_start",
    "line_end",
    "source",
    "detected_by_gate",
    "escaped_gate",
    "catching_tools",
    "tool_rule_ids",
    "candidate_key",
    "confirmed_status",
    "manual_severity",
    "manual_cwe_id",
    "manual_only",
    "reviewer_notes",
]


def group_findings(findings: list[dict[str, str]]) -> list[dict[str, Any]]:
    groups: dict[str, list[dict[str, str]]] = defaultdict(list)
    for row in findings:
        groups[row["candidate_key"]].append(row)

    output_rows: list[dict[str, Any]] = []
    next_id = 1
    for key, items in sorted(groups.items(), key=lambda kv: (kv[1][0].get("run_id", ""), kv[1][0].get("file_path", ""), kv[1][0].get("start_line", ""))):
        first = items[0]
        line_values = [parse_int(item.get("start_line"), 0) or 0 for item in items]
        end_values = [parse_int(item.get("end_line"), 0) or 0 for item in items]
        cwe_values = [item.get("cwe_id", "") for item in items if item.get("cwe_id")]
        severity_values = [item.get("severity", "") for item in items if item.get("severity")]
        tools = sorted({item.get("tool", "") for item in items if item.get("tool")})
        rule_ids = sorted({item.get("rule_id", "") for item in items if item.get("rule_id")})

        severity_order = {"critical": 4, "high": 3, "medium": 2, "low": 1, "info": 0}
        severity = ""
        if severity_values:
            severity = max(severity_values, key=lambda sev: severity_order.get(sev, -1))

        row = {
            "vulnerability_id": f"V-{next_id:04d}",
            "run_id": first.get("run_id", ""),
            "pr_number": first.get("pr_number", ""),
            "task_id": first.get("task_id", ""),
            "condition": first.get("condition", ""),
            "version": first.get("version", ""),
            "include_in_analysis": first.get("include_in_analysis", "false"),
            "cwe_id": cwe_values[0] if cwe_values else "",
            "severity": severity,
            "file_path": normalize_file_path(first.get("file_path", "")),
            "line_start": min(line_values) if line_values else 0,
            "line_end": max(end_values) if end_values else 0,
            "source": "tool",
            "detected_by_gate": "true",
            "escaped_gate": "false",
            "catching_tools": ";".join(tools),
            "tool_rule_ids": ";".join(rule_ids),
            "candidate_key": key,
            "confirmed_status": "",
            "manual_severity": "",
            "manual_cwe_id": "",
            "manual_only": "false",
            "reviewer_notes": "",
        }
        output_rows.append(row)
        next_id += 1
    return output_rows


def merge_existing(new_rows: list[dict[str, Any]], existing_rows: list[dict[str, str]]) -> list[dict[str, Any]]:
    existing_by_key = {}
    for row in existing_rows:
        key = row.get("candidate_key") or row.get("vulnerability_id")
        if key:
            existing_by_key[key] = row

    merged: list[dict[str, Any]] = []
    seen_existing_keys: set[str] = set()

    for row in new_rows:
        key = row.get("candidate_key") or row.get("vulnerability_id")
        existing = existing_by_key.get(key)
        if existing:
            updated = dict(existing)
            updated.update({k: v for k, v in row.items() if v not in (None, "")})
            merged.append(updated)
            seen_existing_keys.add(key)
        else:
            merged.append(row)

    # Preserve manual-only rows that have no candidate_key match
    for row in existing_rows:
        key = row.get("candidate_key") or row.get("vulnerability_id")
        if key and key not in seen_existing_keys and parse_bool(row.get("manual_only"), False):
            merged.append(row)

    return merged


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build a weakness-level manual triage template from normalized raw findings."
    )
    parser.add_argument(
        "--findings",
        required=True,
        help="Path to findings_raw.csv.",
    )
    parser.add_argument(
        "--output",
        default="data/vulnerabilities.csv",
        help="Path to write the vulnerability triage template CSV.",
    )
    parser.add_argument(
        "--preserve-existing",
        action="store_true",
        help="If the output file already exists, preserve manual review columns and manual-only rows.",
    )
    args = parser.parse_args()

    findings = load_csv_rows(Path(args.findings))
    if not findings:
        raise SystemExit(f"No findings found in: {args.findings}")

    new_rows = group_findings(findings)
    output_path = Path(args.output)
    if args.preserve_existing and output_path.exists():
        existing_rows = load_csv_rows(output_path)
        new_rows = merge_existing(new_rows, existing_rows)

    write_csv_rows(output_path, FIELDS, new_rows)
    print(f"Vulnerability template written to: {output_path}")
    print(f"Rows created:                    {len(new_rows)}")
    print("\nNext step: open the CSV and fill at least these columns:")
    print("  - confirmed_status   (TP / FP / UNSURE)")
    print("  - reviewer_notes")
    print("  - manual_severity / manual_cwe_id when the tool metadata is wrong")
    print("  - add manual_only=true rows for vulnerabilities found only in manual review")


if __name__ == "__main__":
    main()
