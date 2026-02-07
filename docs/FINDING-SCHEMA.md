# Finding Schema and Deduplication Policy

## Purpose

Define upfront how findings are recorded and deduplicated so that
triage decisions are consistent across all 18 PRs.

---

## Finding Record Schema

Each row in `data/findings.csv` represents one tool finding:

| Field | Type | Description |
|-------|------|-------------|
| `finding_id` | string | Auto-increment: `F-001`, `F-002`, ... |
| `pr_number` | int | GitHub PR number |
| `task_id` | string | `T1`–`T6` |
| `condition` | string | `human-only`, `low-ai`, `high-ai` |
| `ai_share` | float | Measured AI Share for this PR (0.0–1.0) |
| `pipeline` | string | `baseline` or `security-gated` |
| `tool` | string | `codeql`, `semgrep`, `gitleaks`, `depcheck`, `manual` |
| `rule_id` | string | Tool-specific rule ID (e.g., `java/sql-injection`) |
| `cwe_id` | string | CWE number (e.g., `CWE-89`) |
| `severity` | string | `critical`, `high`, `medium`, `low`, `info` |
| `file_path` | string | Relative path from repo root |
| `start_line` | int | Line number of finding |
| `fingerprint` | string | See "Fingerprint" below |
| `triage_status` | string | `TP` (true positive), `FP` (false positive), `unknown` |
| `is_cross_tool_dup` | bool | `true` if another tool reported the same weakness |
| `unique_weakness_id` | string | `W-001`, `W-002`, ... (see "Unique Weaknesses") |
| `triage_notes` | string | Free-text explanation of triage decision |

---

## Fingerprint Computation

Each finding gets a fingerprint for deduplication:

```
fingerprint = hash(tool + rule_id + file_path + start_line)
```

This is used for **within-tool** deduplication only (e.g., if the same
PR is scanned twice and CodeQL reports the same finding both times).

---

## Deduplication Rules

### Rule 1: Same tool, same location = same finding

If two entries share the same `tool + rule_id + file_path + start_line`,
they are the same finding. Keep only one row.

### Rule 2: Cross-tool overlap = separate findings, one unique weakness

When multiple tools flag the same code location for the same weakness:

- Record each as a separate finding row (different `finding_id`)
- Mark `is_cross_tool_dup = true` on the secondary finding(s)
- Assign the same `unique_weakness_id` to all related findings

**Example:**
Both CodeQL and Semgrep flag SQL injection on `SearchService.java:42`.

| finding_id | tool | rule_id | unique_weakness_id | is_cross_tool_dup |
|-----------|------|---------|-------------------|-------------------|
| F-012 | codeql | java/sql-injection | W-008 | false |
| F-013 | semgrep | java.lang.security.audit.sqli | W-008 | true |

For CWE counting and severity analysis, use **unique weaknesses** (W-IDs),
not raw finding counts. This prevents double-counting.

### Rule 3: Manual review findings

If manual review discovers a weakness that no tool found:
- Record with `tool = manual`
- This represents a false negative for all automated tools
- Assign a new `unique_weakness_id`

If manual review confirms a tool finding:
- Update the existing finding's `triage_status` to `TP`
- Do NOT create a duplicate manual finding

---

## Unique Weakness Registry

Maintain a separate mental model (or simple lookup) of unique weaknesses:

| unique_weakness_id | cwe_id | severity | file_path | line | description | found_by_tools |
|-------------------|--------|----------|-----------|------|-------------|----------------|
| W-001 | CWE-89 | high | SearchService.java | 42 | SQL concatenation in search query | codeql, semgrep |
| W-002 | CWE-798 | critical | application.yml | 15 | Hardcoded JWT secret | gitleaks, semgrep |
| W-003 | CWE-862 | high | AdminController.java | 28 | Missing auth check on delete | manual |

The unique weakness count per PR is the primary metric for RQ1 and RQ2.

---

## Severity Normalization

Different tools use different severity scales. Normalize to:

| Normalized | CodeQL | Semgrep | OWASP Dep-Check | Gitleaks | Manual |
|-----------|--------|---------|----------------|----------|--------|
| critical | error (CVSS 9+) | ERROR | CVSS >= 9.0 | — | reviewer judgment |
| high | error | ERROR | CVSS 7.0–8.9 | all secrets | reviewer judgment |
| medium | warning | WARNING | CVSS 4.0–6.9 | — | reviewer judgment |
| low | note | INFO | CVSS < 4.0 | — | reviewer judgment |

Gitleaks findings are always classified as `high` — any leaked secret
is a significant security issue regardless of the secret type.

---

## Triage Decision Guide

| Scenario | Triage Status |
|----------|---------------|
| Tool finding points to real vulnerability exploitable in context | `TP` |
| Tool finding is technically correct but not exploitable (e.g., test code, dead path) | `FP` |
| Tool finding flags a pattern that is mitigated elsewhere (e.g., input validated upstream) | `FP` |
| Uncertain after reasonable analysis | `unknown` |

When in doubt, err toward `TP`. It is better to overcount than undercount
for the purpose of this study, as the key question is whether gates catch
issues — not whether every issue is a critical vulnerability.
