# Manual Security Review Checklist

**PR #:** ___  |  **Task:** ___  |  **Condition:** ___  |  **Reviewer:** ___  |  **Date:** ___

## Instructions

Review the PR diff against the checklist below. For each item, mark:
- ✅ **Pass** — properly handled
- ❌ **Fail** — vulnerability present (document in findings.csv)
- ➖ **N/A** — not applicable to this task

---

## A. Input Validation (CWE-20, CWE-89, CWE-79)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| A1 | All user input validated before use | | |
| A2 | No SQL string concatenation (parameterized queries used) | | |
| A3 | No XSS vectors in responses (output encoding) | | |
| A4 | Request size/length limits enforced | | |
| A5 | Enum/whitelist validation for constrained fields | | |

## B. Authentication (CWE-287, CWE-256, CWE-521)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| B1 | Passwords hashed with strong algorithm (bcrypt/argon2) | | |
| B2 | No plaintext password storage or logging | | |
| B3 | Password strength requirements enforced | | |
| B4 | JWT properly validated (signature, expiration, issuer) | | |
| B5 | Generic error messages on auth failure | | |

## C. Authorization (CWE-862, CWE-863, CWE-639)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| C1 | All endpoints have authorization checks | | |
| C2 | No IDOR — users cannot access other users' resources | | |
| C3 | Role checks prevent privilege escalation | | |
| C4 | Admin functions restricted to admin role | | |

## D. Cryptography (CWE-327, CWE-321, CWE-329)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| D1 | Strong algorithms used (AES-256-GCM, not DES/ECB) | | |
| D2 | No hardcoded cryptographic keys | | |
| D3 | Random IV/nonce generation (SecureRandom) | | |
| D4 | Key derived from config/env, not source code | | |

## E. Data Exposure (CWE-200, CWE-209, CWE-532)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| E1 | No sensitive data in log output | | |
| E2 | Error messages don't leak internal details | | |
| E3 | Stack traces not returned to client | | |
| E4 | Sensitive fields excluded from API responses | | |

## F. File Handling (CWE-22, CWE-434, CWE-400)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| F1 | Filenames sanitized (no path traversal) | | |
| F2 | File type validated (not just extension) | | |
| F3 | Upload size limits enforced | | |
| F4 | Storage path uses random/UUID names | | |

## G. Secrets & Configuration (CWE-798, CWE-918)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| G1 | No hardcoded secrets in source code | | |
| G2 | API keys from environment/config only | | |
| G3 | No SSRF vectors in URL construction | | |
| G4 | TLS verification not disabled | | |

---

## Summary

- **Total checks applicable:** ___
- **Pass:** ___
- **Fail:** ___
- **Findings logged in findings.csv:** ___
