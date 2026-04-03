
#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any

from common import (
    CONDITION_ORDER,
    line_bucket,
    load_csv_rows,
    normalize_file_path,
    normalize_whitespace,
    parse_bool,
    parse_int,
    sha256_short,
    write_csv_rows,
)

CODEQL_SEVERITY_MAP = {
    "error": "high",
    "warning": "medium",
    "note": "low",
    "none": "info",
}
SEMGREP_SEVERITY_MAP = {
    "ERROR": "high",
    "WARNING": "medium",
    "INFO": "low",
}
SPOTBUGS_TYPE_TO_CWE = {
    "ECB_MODE": "CWE-327",
    "PREDICTABLE_RANDOM": "CWE-330",
    "HARD_CODE_PASSWORD": "CWE-259",
    "HARD_CODE_KEY": "CWE-321",
    "WEAK_MESSAGE_DIGEST": "CWE-328",
    "WEAK_TRUST_MANAGER": "CWE-295",
    "WEAK_HOSTNAME_VERIFIER": "CWE-297",
    "PATH_TRAVERSAL_IN": "CWE-22",
    "XXE_DOCUMENT": "CWE-611",
    "LDAP_INJECTION": "CWE-90",
    "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE": "CWE-89",
    "COMMAND_INJECTION": "CWE-78",
    "SCRIPT_ENGINE_INJECTION": "CWE-94",
    "URLCONNECTION_SSRF_FD": "CWE-918",
    "HTTP_RESPONSE_SPLITTING": "CWE-113",
    "XSS_SERVLET": "CWE-79",
    "XSS_REQUEST_PARAMETER_TO_SEND_ERROR": "CWE-79",
    "TRUST_BOUNDARY_VIOLATION": "CWE-501",
}

FIELDNAMES = [
    "finding_id",
    "run_id",
    "pr_number",
    "task_id",
    "condition",
    "version",
    "include_in_analysis",
    "pipeline",
    "tool",
    "rule_id",
    "cwe_id",
    "severity",
    "file_path",
    "start_line",
    "end_line",
    "message",
    "fingerprint",
    "candidate_key",
    "artifact_source",
]


def normalize_cvss_severity(score: float) -> str:
    if score >= 9.0:
        return "critical"
    if score >= 7.0:
        return "high"
    if score >= 4.0:
        return "medium"
    if score > 0:
        return "low"
    return "info"


def extract_cwe_from_text(value: str) -> str:
    if not value:
        return ""
    match = re.search(r"CWE[-\s]?(\d+)", str(value), re.IGNORECASE)
    return f"CWE-{match.group(1)}" if match else ""


def candidate_key(row: dict[str, Any]) -> str:
    cwe_or_rule = row.get("cwe_id") or row.get("rule_id") or "UNKNOWN"
    bucket = line_bucket(parse_int(row.get("start_line"), 0) or 0, width=3)
    raw = "|".join(
        [
            str(row.get("run_id", "")),
            normalize_file_path(str(row.get("file_path", ""))),
            str(cwe_or_rule),
            str(bucket),
        ]
    )
    return sha256_short(raw)


def compute_fingerprint(row: dict[str, Any]) -> str:
    raw = "|".join(
        [
            str(row.get("run_id", "")),
            str(row.get("tool", "")),
            str(row.get("rule_id", "")),
            normalize_file_path(str(row.get("file_path", ""))),
            str(row.get("start_line", "")),
        ]
    )
    return sha256_short(raw)


def severity_from_spotbugs(rank: int | None, priority: int | None) -> str:
    if rank is not None:
        if rank <= 4:
            return "high"
        if rank <= 9:
            return "medium"
        return "low"
    if priority is not None:
        if priority == 1:
            return "high"
        if priority == 2:
            return "medium"
        return "low"
    return "medium"


def parse_codeql_sarif(path: Path) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    sarif = json.loads(path.read_text(encoding="utf-8", errors="replace"))
    for run in sarif.get("runs", []):
        rules = {
            rule.get("id", ""): rule
            for rule in run.get("tool", {}).get("driver", {}).get("rules", [])
        }
        for result in run.get("results", []):
            rule_id = result.get("ruleId", "")
            rule = rules.get(rule_id, {})
            locations = result.get("locations", [])
            location = locations[0] if locations else {}
            physical = location.get("physicalLocation", {})
            region = physical.get("region", {})
            cwe = extract_cwe_from_text(json.dumps(rule.get("properties", {})))
            if not cwe:
                cwe = extract_cwe_from_text(json.dumps(result))
            findings.append(
                {
                    "tool": "codeql",
                    "rule_id": rule_id,
                    "cwe_id": cwe,
                    "severity": CODEQL_SEVERITY_MAP.get(result.get("level", "warning"), "medium"),
                    "file_path": normalize_file_path(
                        physical.get("artifactLocation", {}).get("uri", "")
                    ),
                    "start_line": region.get("startLine", 0),
                    "end_line": region.get("endLine", region.get("startLine", 0)),
                    "message": normalize_whitespace(result.get("message", {}).get("text", ""))[:400],
                    "artifact_source": str(path),
                }
            )
    return findings


def parse_semgrep_json(path: Path) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    data = json.loads(path.read_text(encoding="utf-8", errors="replace"))
    for result in data.get("results", []):
        metadata = result.get("extra", {}).get("metadata", {})
        cwe = ""
        if "cwe" in metadata:
            cwe = extract_cwe_from_text(json.dumps(metadata.get("cwe")))
        if not cwe:
            cwe = extract_cwe_from_text(json.dumps(result))
        findings.append(
            {
                "tool": "semgrep",
                "rule_id": result.get("check_id", ""),
                "cwe_id": cwe,
                "severity": SEMGREP_SEVERITY_MAP.get(
                    str(result.get("extra", {}).get("severity", "WARNING")).upper(), "medium"
                ),
                "file_path": normalize_file_path(result.get("path", "")),
                "start_line": result.get("start", {}).get("line", 0),
                "end_line": result.get("end", {}).get("line", result.get("start", {}).get("line", 0)),
                "message": normalize_whitespace(result.get("extra", {}).get("message", ""))[:400],
                "artifact_source": str(path),
            }
        )
    return findings


def parse_semgrep_sarif(path: Path) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    sarif = json.loads(path.read_text(encoding="utf-8", errors="replace"))
    for run in sarif.get("runs", []):
        rules = {
            rule.get("id", ""): rule
            for rule in run.get("tool", {}).get("driver", {}).get("rules", [])
        }
        for result in run.get("results", []):
            rule_id = result.get("ruleId", "")
            rule = rules.get(rule_id, {})
            locations = result.get("locations", [])
            location = locations[0] if locations else {}
            physical = location.get("physicalLocation", {})
            region = physical.get("region", {})
            cwe = extract_cwe_from_text(json.dumps(rule.get("properties", {})))
            if not cwe:
                cwe = extract_cwe_from_text(json.dumps(result))
            findings.append(
                {
                    "tool": "semgrep",
                    "rule_id": rule_id,
                    "cwe_id": cwe,
                    "severity": CODEQL_SEVERITY_MAP.get(result.get("level", "warning"), "medium"),
                    "file_path": normalize_file_path(
                        physical.get("artifactLocation", {}).get("uri", "")
                    ),
                    "start_line": region.get("startLine", 0),
                    "end_line": region.get("endLine", region.get("startLine", 0)),
                    "message": normalize_whitespace(result.get("message", {}).get("text", ""))[:400],
                    "artifact_source": str(path),
                }
            )
    return findings


def parse_gitleaks_sarif(path: Path) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    sarif = json.loads(path.read_text(encoding="utf-8", errors="replace"))
    for run in sarif.get("runs", []):
        for result in run.get("results", []):
            locations = result.get("locations", [])
            location = locations[0] if locations else {}
            physical = location.get("physicalLocation", {})
            region = physical.get("region", {})
            findings.append(
                {
                    "tool": "gitleaks",
                    "rule_id": result.get("ruleId", ""),
                    "cwe_id": "CWE-798",
                    "severity": "high",
                    "file_path": normalize_file_path(
                        physical.get("artifactLocation", {}).get("uri", "")
                    ),
                    "start_line": region.get("startLine", 0),
                    "end_line": region.get("endLine", region.get("startLine", 0)),
                    "message": normalize_whitespace(result.get("message", {}).get("text", ""))[:400],
                    "artifact_source": str(path),
                }
            )
    return findings


def parse_depcheck_json(path: Path) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    data = json.loads(path.read_text(encoding="utf-8", errors="replace"))
    for dep in data.get("dependencies", []):
        for vuln in dep.get("vulnerabilities", []) or []:
            cwe = ""
            cwes = vuln.get("cwes") or []
            if cwes:
                cwe = extract_cwe_from_text(str(cwes[0]))
            cvss3 = vuln.get("cvssv3") or {}
            cvss2 = vuln.get("cvssv2") or {}
            score = cvss3.get("baseScore") or cvss2.get("score") or 0.0
            findings.append(
                {
                    "tool": "dependency-check",
                    "rule_id": vuln.get("name", ""),
                    "cwe_id": cwe,
                    "severity": normalize_cvss_severity(float(score or 0.0)),
                    "file_path": normalize_file_path(dep.get("fileName", "")),
                    "start_line": 0,
                    "end_line": 0,
                    "message": normalize_whitespace(vuln.get("description", ""))[:400],
                    "artifact_source": str(path),
                }
            )
    return findings


def parse_spotbugs_xml(path: Path) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    tree = ET.parse(path)
    root = tree.getroot()
    for bug in root.findall(".//BugInstance"):
        source_line = bug.find("SourceLine")
        file_path = ""
        start_line = 0
        end_line = 0
        if source_line is not None:
            file_path = source_line.attrib.get("sourcepath", "")
            start_line = parse_int(source_line.attrib.get("start"), 0) or 0
            end_line = parse_int(source_line.attrib.get("end"), start_line) or start_line

        bug_type = bug.attrib.get("type", "")
        cwe = bug.attrib.get("cweid", "")
        if cwe and not cwe.startswith("CWE-"):
            cwe = f"CWE-{cwe}"
        if not cwe:
            cwe = SPOTBUGS_TYPE_TO_CWE.get(bug_type, "")

        short_message = bug.findtext("ShortMessage", default="")
        long_message = bug.findtext("LongMessage", default=short_message)
        rank = parse_int(bug.attrib.get("rank"))
        priority = parse_int(bug.attrib.get("priority"))
        findings.append(
            {
                "tool": "spotbugs",
                "rule_id": bug_type,
                "cwe_id": cwe,
                "severity": severity_from_spotbugs(rank, priority),
                "file_path": normalize_file_path(file_path),
                "start_line": start_line,
                "end_line": end_line,
                "message": normalize_whitespace(long_message or short_message)[:400],
                "artifact_source": str(path),
            }
        )
    return findings


def deduplicate_exact(findings: list[dict[str, Any]]) -> list[dict[str, Any]]:
    seen: set[tuple[Any, ...]] = set()
    deduped: list[dict[str, Any]] = []
    for item in findings:
        key = (
            item.get("run_id"),
            item.get("pipeline"),
            item.get("tool"),
            item.get("rule_id"),
            item.get("file_path"),
            item.get("start_line"),
            item.get("message"),
        )
        if key in seen:
            continue
        seen.add(key)
        deduped.append(item)
    return deduped


def parse_run(row: dict[str, str]) -> list[dict[str, Any]]:
    parsers = [
        ("codeql_sarif", parse_codeql_sarif),
        ("semgrep_json", parse_semgrep_json),
        ("semgrep_sarif", parse_semgrep_sarif),
        ("spotbugs_xml", parse_spotbugs_xml),
        ("gitleaks_sarif", parse_gitleaks_sarif),
        ("depcheck_json", parse_depcheck_json),
    ]
    findings: list[dict[str, Any]] = []
    for column, parser in parsers:
        path_value = row.get(column, "")
        if not path_value:
            continue
        path = Path(path_value)
        if not path.exists():
            continue
        try:
            parsed = parser(path)
        except Exception as exc:
            print(f"{row.get('run_id')}: failed to parse {column} ({exc})")
            continue
        for finding in parsed:
            finding.update(
                {
                    "run_id": row.get("run_id", ""),
                    "pr_number": row.get("pr_number", ""),
                    "task_id": row.get("task_id", ""),
                    "condition": row.get("condition", ""),
                    "version": row.get("version", ""),
                    "include_in_analysis": str(parse_bool(row.get("include_in_analysis"), False)).lower(),
                    "pipeline": "security-gated",
                }
            )
            finding["fingerprint"] = compute_fingerprint(finding)
            finding["candidate_key"] = candidate_key(finding)
            findings.append(finding)
    return deduplicate_exact(findings)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Batch-parse scanner artifacts from thesis result folders into findings_raw.csv."
    )
    parser.add_argument(
        "--manifest",
        required=True,
        help="Path to runs.csv manifest.",
    )
    parser.add_argument(
        "--output",
        default="data/findings_raw.csv",
        help="Path to write the normalized raw findings CSV.",
    )
    parser.add_argument(
        "--include-excluded",
        action="store_true",
        help="Parse excluded runs too. By default, only include_in_analysis=true rows are processed.",
    )
    args = parser.parse_args()

    manifest_rows = load_csv_rows(Path(args.manifest))
    if not manifest_rows:
        raise SystemExit(f"No rows found in manifest: {args.manifest}")

    rows: list[dict[str, Any]] = []
    next_id = 1
    for manifest_row in manifest_rows:
        if not args.include_excluded and not parse_bool(manifest_row.get("include_in_analysis"), False):
            continue
        parsed = parse_run(manifest_row)
        for finding in parsed:
            finding["finding_id"] = f"F-{next_id:04d}"
            next_id += 1
        rows.extend(parsed)
        print(f"{manifest_row.get('run_id')}: {len(parsed)} raw finding(s)")

    write_csv_rows(Path(args.output), FIELDNAMES, rows)
    print(f"\nRaw findings written to: {args.output}")
    print(f"Total raw findings:      {len(rows)}")


if __name__ == "__main__":
    main()
