#!/usr/bin/env python3
"""
Findings Parser and Normalizer

Parses output from CodeQL (SARIF), Semgrep (SARIF/JSON), Gitleaks (SARIF),
and OWASP Dependency-Check (JSON) into a unified findings.csv schema.

Computes fingerprints and flags cross-tool duplicates.

Usage:
    python parse_findings.py \
        --pr 3 \
        --task T1 \
        --condition high-ai \
        --ai-share 0.82 \
        --pipeline security-gated \
        --codeql-sarif artifacts/codeql-results/java.sarif \
        --semgrep-json artifacts/semgrep-results.json \
        --gitleaks-sarif artifacts/gitleaks-results.sarif \
        --depcheck-json artifacts/dependency-check-report.json \
        --output data/findings.csv
"""

import argparse
import csv
import hashlib
import json
import os
import re
import sys
from pathlib import Path


# ── Severity normalization ──────────────────────────────────────────────

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

def normalize_cvss_severity(cvss_score: float) -> str:
    if cvss_score >= 9.0:
        return "critical"
    elif cvss_score >= 7.0:
        return "high"
    elif cvss_score >= 4.0:
        return "medium"
    else:
        return "low"


# ── Fingerprint ─────────────────────────────────────────────────────────

def compute_fingerprint(tool: str, rule_id: str, file_path: str, start_line: int) -> str:
    raw = f"{tool}|{rule_id}|{file_path}|{start_line}"
    return hashlib.sha256(raw.encode()).hexdigest()[:16]


# ── CWE extraction ──────────────────────────────────────────────────────

def extract_cwe_from_tags(tags: list) -> str:
    """Extract CWE ID from SARIF tags like ['external/cwe/cwe-089']."""
    for tag in tags:
        match = re.search(r"cwe-?(\d+)", str(tag), re.IGNORECASE)
        if match:
            return f"CWE-{match.group(1)}"
    return ""

def extract_cwe_from_text(text: str) -> str:
    """Extract CWE ID from free text."""
    match = re.search(r"CWE-(\d+)", text, re.IGNORECASE)
    if match:
        return f"CWE-{match.group(1)}"
    return ""


# ── Parsers ─────────────────────────────────────────────────────────────

def parse_codeql_sarif(sarif_path: str) -> list[dict]:
    """Parse CodeQL SARIF output."""
    findings = []
    with open(sarif_path) as f:
        sarif = json.load(f)

    for run in sarif.get("runs", []):
        # Build rule lookup
        rules = {}
        tool_section = run.get("tool", {}).get("driver", {})
        for rule in tool_section.get("rules", []):
            rules[rule["id"]] = rule

        for result in run.get("results", []):
            rule_id = result.get("ruleId", "unknown")
            rule_def = rules.get(rule_id, {})

            # Location
            locations = result.get("locations", [{}])
            phys = locations[0].get("physicalLocation", {}) if locations else {}
            file_path = phys.get("artifactLocation", {}).get("uri", "")
            start_line = phys.get("region", {}).get("startLine", 0)

            # Severity
            level = result.get("level", "warning")
            severity = CODEQL_SEVERITY_MAP.get(level, "medium")

            # CWE
            tags = rule_def.get("properties", {}).get("tags", [])
            cwe = extract_cwe_from_tags(tags)
            if not cwe:
                cwe = extract_cwe_from_text(result.get("message", {}).get("text", ""))

            findings.append({
                "tool": "codeql",
                "rule_id": rule_id,
                "cwe_id": cwe,
                "severity": severity,
                "file_path": file_path,
                "start_line": start_line,
                "description": result.get("message", {}).get("text", "")[:200],
            })

    return findings


def parse_semgrep_json(json_path: str) -> list[dict]:
    """Parse Semgrep JSON output."""
    findings = []
    with open(json_path) as f:
        data = json.load(f)

    for result in data.get("results", []):
        rule_id = result.get("check_id", "unknown")
        severity = SEMGREP_SEVERITY_MAP.get(
            result.get("extra", {}).get("severity", "WARNING"), "medium"
        )

        # CWE from metadata
        metadata = result.get("extra", {}).get("metadata", {})
        cwe_list = metadata.get("cwe", [])
        cwe = ""
        if cwe_list:
            cwe = extract_cwe_from_text(str(cwe_list[0])) if isinstance(cwe_list, list) else extract_cwe_from_text(str(cwe_list))

        findings.append({
            "tool": "semgrep",
            "rule_id": rule_id,
            "cwe_id": cwe,
            "severity": severity,
            "file_path": result.get("path", ""),
            "start_line": result.get("start", {}).get("line", 0),
            "description": result.get("extra", {}).get("message", "")[:200],
        })

    return findings


def parse_semgrep_sarif(sarif_path: str) -> list[dict]:
    """Parse Semgrep SARIF output (fallback if JSON not available)."""
    findings = []
    with open(sarif_path) as f:
        sarif = json.load(f)

    for run in sarif.get("runs", []):
        rules = {}
        for rule in run.get("tool", {}).get("driver", {}).get("rules", []):
            rules[rule["id"]] = rule

        for result in run.get("results", []):
            rule_id = result.get("ruleId", "unknown")
            rule_def = rules.get(rule_id, {})

            locations = result.get("locations", [{}])
            phys = locations[0].get("physicalLocation", {}) if locations else {}
            file_path = phys.get("artifactLocation", {}).get("uri", "")
            start_line = phys.get("region", {}).get("startLine", 0)

            level = result.get("level", "warning")
            severity = CODEQL_SEVERITY_MAP.get(level, "medium")  # SARIF levels are the same

            tags = rule_def.get("properties", {}).get("tags", [])
            cwe = extract_cwe_from_tags(tags)

            findings.append({
                "tool": "semgrep",
                "rule_id": rule_id,
                "cwe_id": cwe,
                "severity": severity,
                "file_path": file_path,
                "start_line": start_line,
                "description": result.get("message", {}).get("text", "")[:200],
            })

    return findings


def parse_gitleaks_sarif(sarif_path: str) -> list[dict]:
    """Parse Gitleaks SARIF output. All secrets are severity=high."""
    findings = []
    with open(sarif_path) as f:
        sarif = json.load(f)

    for run in sarif.get("runs", []):
        for result in run.get("results", []):
            locations = result.get("locations", [{}])
            phys = locations[0].get("physicalLocation", {}) if locations else {}
            file_path = phys.get("artifactLocation", {}).get("uri", "")
            start_line = phys.get("region", {}).get("startLine", 0)

            findings.append({
                "tool": "gitleaks",
                "rule_id": result.get("ruleId", "unknown"),
                "cwe_id": "CWE-798",  # Hardcoded credentials
                "severity": "high",
                "file_path": file_path,
                "start_line": start_line,
                "description": result.get("message", {}).get("text", "")[:200],
            })

    return findings


def parse_depcheck_json(json_path: str) -> list[dict]:
    """Parse OWASP Dependency-Check JSON report."""
    findings = []
    with open(json_path) as f:
        data = json.load(f)

    for dep in data.get("dependencies", []):
        for vuln in dep.get("vulnerabilities", []):
            cvss_score = 0.0
            # Try CVSSv3 first, then v2
            cvss3 = vuln.get("cvssv3", {})
            cvss2 = vuln.get("cvssv2", {})
            if cvss3:
                cvss_score = cvss3.get("baseScore", 0.0)
            elif cvss2:
                cvss_score = cvss2.get("score", 0.0)

            # CWE
            cwes = vuln.get("cwes", [])
            cwe = f"CWE-{cwes[0]}" if cwes else ""

            findings.append({
                "tool": "depcheck",
                "rule_id": vuln.get("name", "unknown"),
                "cwe_id": cwe,
                "severity": normalize_cvss_severity(cvss_score),
                "file_path": dep.get("fileName", ""),
                "start_line": 0,  # Not applicable for dependencies
                "description": vuln.get("description", "")[:200],
            })

    return findings


# ── Deduplication ───────────────────────────────────────────────────────

def deduplicate_and_assign_weaknesses(findings: list[dict]) -> list[dict]:
    """
    1. Within-tool dedup: same tool + rule_id + file + line = same finding
    2. Cross-tool: same file + similar line (±3) + same CWE = same weakness
    """

    # Step 1: Within-tool dedup
    seen = set()
    deduped = []
    for f in findings:
        key = (f["tool"], f["rule_id"], f["file_path"], f["start_line"])
        if key not in seen:
            seen.add(key)
            deduped.append(f)

    # Step 2: Assign unique weakness IDs and detect cross-tool overlaps
    weakness_counter = 0
    weakness_map = []  # list of (cwe, file, line_range, weakness_id)

    for f in deduped:
        matched_weakness = None
        for wm in weakness_map:
            if (
                f["cwe_id"]
                and f["cwe_id"] == wm["cwe"]
                and f["file_path"] == wm["file"]
                and abs(f["start_line"] - wm["line"]) <= 3
            ):
                matched_weakness = wm
                break

        if matched_weakness:
            f["unique_weakness_id"] = matched_weakness["weakness_id"]
            f["is_cross_tool_dup"] = True
            # Update line range
            matched_weakness["line"] = min(matched_weakness["line"], f["start_line"])
        else:
            weakness_counter += 1
            wid = f"W-{weakness_counter:03d}"
            f["unique_weakness_id"] = wid
            f["is_cross_tool_dup"] = False
            weakness_map.append({
                "cwe": f["cwe_id"],
                "file": f["file_path"],
                "line": f["start_line"],
                "weakness_id": wid,
            })

    return deduped


# ── Main ────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Parse and normalize security findings")
    parser.add_argument("--pr", type=int, required=True)
    parser.add_argument("--task", required=True, help="Task ID (T1–T6)")
    parser.add_argument("--condition", required=True, choices=["human-only", "low-ai", "high-ai"])
    parser.add_argument("--ai-share", type=float, required=True)
    parser.add_argument("--pipeline", required=True, choices=["baseline", "security-gated"])
    parser.add_argument("--codeql-sarif", help="Path to CodeQL SARIF file")
    parser.add_argument("--semgrep-json", help="Path to Semgrep JSON file")
    parser.add_argument("--semgrep-sarif", help="Path to Semgrep SARIF file (fallback)")
    parser.add_argument("--gitleaks-sarif", help="Path to Gitleaks SARIF file")
    parser.add_argument("--depcheck-json", help="Path to Dependency-Check JSON file")
    parser.add_argument("--output", default="data/findings.csv", help="Output CSV path")
    args = parser.parse_args()

    all_findings = []

    # Parse each tool's output
    if args.codeql_sarif and os.path.exists(args.codeql_sarif):
        codeql = parse_codeql_sarif(args.codeql_sarif)
        all_findings.extend(codeql)
        print(f"  CodeQL:    {len(codeql)} finding(s)")

    if args.semgrep_json and os.path.exists(args.semgrep_json):
        semgrep = parse_semgrep_json(args.semgrep_json)
        all_findings.extend(semgrep)
        print(f"  Semgrep:   {len(semgrep)} finding(s)")
    elif args.semgrep_sarif and os.path.exists(args.semgrep_sarif):
        semgrep = parse_semgrep_sarif(args.semgrep_sarif)
        all_findings.extend(semgrep)
        print(f"  Semgrep:   {len(semgrep)} finding(s) (from SARIF)")

    if args.gitleaks_sarif and os.path.exists(args.gitleaks_sarif):
        gitleaks = parse_gitleaks_sarif(args.gitleaks_sarif)
        all_findings.extend(gitleaks)
        print(f"  Gitleaks:  {len(gitleaks)} finding(s)")

    if args.depcheck_json and os.path.exists(args.depcheck_json):
        depcheck = parse_depcheck_json(args.depcheck_json)
        all_findings.extend(depcheck)
        print(f"  Dep-Check: {len(depcheck)} finding(s)")

    if not all_findings:
        print("  No findings from any tool.")

    # Add PR metadata to each finding
    for f in all_findings:
        f["pr_number"] = args.pr
        f["task_id"] = args.task
        f["condition"] = args.condition
        f["ai_share"] = args.ai_share
        f["pipeline"] = args.pipeline
        f["fingerprint"] = compute_fingerprint(
            f["tool"], f["rule_id"], f["file_path"], f["start_line"]
        )
        f["triage_status"] = ""
        f["triage_notes"] = ""

    # Deduplicate and assign weakness IDs
    all_findings = deduplicate_and_assign_weaknesses(all_findings)

    # Assign finding IDs
    # Check existing file for last ID
    next_id = 1
    if os.path.exists(args.output):
        with open(args.output) as f:
            reader = csv.DictReader(f)
            for row in reader:
                fid = row.get("finding_id", "F-000")
                num = int(fid.split("-")[1])
                next_id = max(next_id, num + 1)

    for f in all_findings:
        f["finding_id"] = f"F-{next_id:03d}"
        next_id += 1

    # Write output
    fieldnames = [
        "finding_id", "pr_number", "task_id", "condition", "ai_share",
        "pipeline", "tool", "rule_id", "cwe_id", "severity", "file_path",
        "start_line", "fingerprint", "triage_status", "is_cross_tool_dup",
        "unique_weakness_id", "triage_notes",
    ]

    file_exists = os.path.exists(args.output)
    with open(args.output, "a", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        if not file_exists:
            writer.writeheader()
        writer.writerows(all_findings)

    # Summary
    unique_weaknesses = len(set(f["unique_weakness_id"] for f in all_findings))
    cross_tool = sum(1 for f in all_findings if f["is_cross_tool_dup"])
    print(f"\n  Total findings:      {len(all_findings)}")
    print(f"  Unique weaknesses:   {unique_weaknesses}")
    print(f"  Cross-tool overlaps: {cross_tool}")
    print(f"  Written to:          {args.output}")


if __name__ == "__main__":
    main()
