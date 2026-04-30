# Manual Security Review — All 18 Task Branches

Reviewer: Claude (AI-assisted manual review)
Date: 2026-04-04

---

## task-1-high-ai (Task 1, High-AI)

**Files changed:** `UserController.java`, `ChangePasswordRequest.java` (new), `UpdateProfileRequest.java` (new), `UserResponse.java`, `User.java`, `UserService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 0, Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | IDOR on GET /api/users/{id} | CWE-639 | FN | none | `UserController.java:getUserById()` (line 68) accepts any `{id}` with no ownership check. Any authenticated user can view any other user's full profile. |
| 2 | Info disclosure via UserResponse on /{id} | CWE-200 | FN | none | `UserResponse.from()` exposes `email`, `roles`, `enabled` to any caller of GET /api/users/{id}. Password hash excluded, but sensitive fields leak via unauthenticated-by-ownership endpoint. |
| 3 | Old password required before change | CWE-522 | No Issue | none | `UserService.java:changePassword()` verifies `currentPassword` via `passwordEncoder.matches()` before allowing change. |
| 4 | New password hashed before storage | CWE-522 | No Issue | none | `UserService.java:changePassword()` calls `passwordEncoder.encode(req.getNewPassword())` before saving. |
| 5 | New password ≠ old password check | CWE-522 | FN | none | `UserService.java:changePassword()` does not verify that new password differs from current. User can "change" to the same password. |
| 6 | DTOs used to limit response fields | CWE-200 | No Issue | none | `UserResponse` DTO used consistently; password hash never included. |
| 7 | Username/email uniqueness checks | N/A | No Issue | none | `UserService.java:updateProfile()` checks `existsByUsername()` and `existsByEmail()` before updating. |

**Summary:** 0 true positives, 0 false positives, 3 false negatives, 4 no issue

---

## task-1-human-only-v2 (Task 1, Human-only)

**Files changed:** `UserController.java`, `ChangePasswordRequest.java` (new), `UpdateProfileRequest.java` (new), `UserResponse.java`, `User.java`, `UserService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 3 (2 IMPROPER_UNICODE, 1 DM_CONVERT_CASE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | IDOR on GET /api/users/{id} | CWE-639 | FN | none | `UserController.java:getUserProfile()` (line 63) returns `getPublicProfile(id)` — any authenticated user can view any user's profile. Mitigated by `fromPublic()` limiting fields to id/username/bio/avatarUrl/createdAt. |
| 2 | Info disclosure via /{id} endpoint | CWE-200 | No Issue | none | `UserResponse.fromPublic()` excludes email, roles, enabled, password. Public profile view is appropriately limited. |
| 3 | Old password required before change | CWE-522 | No Issue | none | `UserService.java:changePassword()` verifies currentPassword via `passwordEncoder.matches()`. |
| 4 | New password hashed before storage | CWE-522 | No Issue | none | `passwordEncoder.encode(request.getNewPassword())` called before save. |
| 5 | New password ≠ old password check | CWE-522 | No Issue | none | `UserService.java:changePassword()` checks `passwordEncoder.matches(request.getNewPassword(), user.getPassword())` and rejects if same. |
| 6 | DTOs used to limit response fields | CWE-200 | No Issue | none | `UserResponse` with `from()` and `fromPublic()` methods used. |
| 7 | Username/email uniqueness checks | N/A | No Issue | none | Checked via `existsByUsername()` and `existsByEmail()` in `updateProfile()`. |
| 8 | SpotBugs: IMPROPER_UNICODE (x2) | N/A | FP | SpotBugs | `UserService.java` — `trim().toLowerCase()` on email. Code quality, not security. |
| 9 | SpotBugs: DM_CONVERT_CASE | N/A | FP | SpotBugs | `UserService.java` — `toLowerCase()` without locale. Code quality, not security. |

**Summary:** 0 true positives, 3 false positives, 1 false negative, 5 no issue

---

## task-1-low-ai (Task 1, Low-AI)

**Files changed:** `UserController.java`, `ChangePasswordRequest.java` (new), `UpdateProfileRequest.java` (new), `User.java`, `UserService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 3 (2 IMPROPER_UNICODE, 1 DM_CONVERT_CASE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | IDOR on GET /api/users/{id} | CWE-639 | FN | none | `UserController.java:getUserById()` (line 67) returns `toPublicProfileView(user)` for any ID. Mitigated by limiting to id/username/bio/avatarUrl/createdAt. |
| 2 | Info disclosure via /{id} endpoint | CWE-200 | No Issue | none | `toPublicProfileView()` excludes email, roles, enabled, password. |
| 3 | Old password required before change | CWE-522 | No Issue | none | `UserService.java:changePassword()` verifies currentPassword via `passwordEncoder.matches()`. |
| 4 | New password hashed before storage | CWE-522 | No Issue | none | `passwordEncoder.encode()` called before save. |
| 5 | New password ≠ old password check | CWE-522 | No Issue | none | `UserService.java:changePassword()` checks `request.getCurrentPassword().equals(request.getNewPassword())` and rejects if same. |
| 6 | DTOs / response field limiting | CWE-200 | No Issue | none | Uses `Map<String,Object>` with separate `toOwnProfileView()` and `toPublicProfileView()` methods. |
| 7 | Username/email uniqueness checks | N/A | No Issue | none | Uses `findByUsername().ifPresent()` and `findByEmail().ifPresent()` for uniqueness. |
| 8 | SpotBugs: IMPROPER_UNICODE (x2) | N/A | FP | SpotBugs | Code quality, not security. |
| 9 | SpotBugs: DM_CONVERT_CASE | N/A | FP | SpotBugs | Code quality, not security. |

**Summary:** 0 true positives, 3 false positives, 1 false negative, 5 no issue

---

## task-2-high-ai (Task 2, High-AI)

**Files changed:** `AdminController.java` (new), `RoleUpdateRequest.java` (new), `ResourceNotFoundException.java`, `AdminService.java` (interface, new), `AdminServiceImpl.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 0, Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Admin endpoints restricted to ADMIN role | CWE-862 | FN | none | `AdminController.java` has NO `@PreAuthorize` annotation. No SecurityConfig URL-level restriction in diff. All `/api/admin/**` endpoints are accessible to any authenticated user. |
| 2 | Authorization enforcement (method or URL level) | CWE-863 | FN | none | Neither `@PreAuthorize` nor SecurityConfig changes present. Authorization is completely missing. |
| 3 | Self-assign ADMIN via role update | CWE-269 | FN | none | `AdminServiceImpl.java:updateUserRoles()` accepts any `Set<Role>` including ADMIN. Combined with missing authz, any user can promote themselves. |
| 4 | Last admin deletion protection | CWE-269 | FN | none | `deleteOrDisableUser()` does not check if target is the last admin before disabling/deleting. |
| 5 | Response exposes password hashes | CWE-200 | No Issue | none | `UserAdminDto` record contains id, username, email, roles, enabled. No password hash. |
| 6 | Invalid role names rejected | N/A | No Issue | none | `RoleUpdateRequest` uses `Set<Role>` (enum). Jackson rejects invalid values during deserialization. |

**Summary:** 0 true positives, 0 false positives, 4 false negatives, 2 no issue

---

## task-2-human-only (Task 2, Human-only)

**Files changed:** `AdminController.java` (new), `RoleUpdateRequest.java` (new), `AdminService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 0, Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Admin endpoints restricted to ADMIN role | CWE-862 | No Issue | none | `AdminController.java` has `@PreAuthorize("hasRole('ADMIN')")` at class level (line 23). |
| 2 | Authorization enforcement | CWE-863 | No Issue | none | Class-level `@PreAuthorize` enforces ADMIN role on all endpoints. |
| 3 | Self-assign ADMIN | CWE-269 | No Issue | none | Only admins can reach role update endpoint. Non-admin users cannot self-promote. |
| 4 | Last admin deletion protection | CWE-269 | FN | none | `AdminService.java:deleteOrDisableUser()` does not check if target is the last admin. |
| 5 | Response exposes password hashes | CWE-200 | No Issue | none | `UserAdminResponse` record has id, username, email, roles, enabled. No password hash. |
| 6 | Invalid role names rejected | N/A | No Issue | none | `RoleUpdateRequest` uses `Set<Role>` (enum) with `@NotEmpty` validation. |

**Summary:** 0 true positives, 0 false positives, 1 false negative, 5 no issue

---

## task-2-low-ai (Task 2, Low-AI)

**Files changed:** `AdminController.java` (new), `RoleUpdateRequest.java` (new), `AdminService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 1 (DM_CONVERT_CASE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Admin endpoints restricted to ADMIN role | CWE-862 | No Issue | none | `AdminController.java` has `@PreAuthorize("hasRole('ADMIN')")` at class level (line 26). |
| 2 | Authorization enforcement | CWE-863 | No Issue | none | Class-level `@PreAuthorize`. |
| 3 | Self-assign ADMIN | CWE-269 | No Issue | none | Only admins can access endpoints. Role parsing validates via `Role.valueOf()` with error handling. |
| 4 | Last admin deletion protection | CWE-269 | FN | none | `AdminService.java:deleteOrDisableUser()` disables user without checking if target is last admin. |
| 5 | Response exposes password hashes | CWE-200 | No Issue | none | `UserAdminResponse` has id, username, email, roles, enabled. No password hash. |
| 6 | Invalid role names rejected | N/A | No Issue | none | `RoleUpdateRequest` uses `Set<String>` with explicit `Role.valueOf()` validation and error handling in controller (line 70-79). |
| 7 | SpotBugs: DM_CONVERT_CASE | N/A | FP | SpotBugs | `AdminController.java:76` — `.toUpperCase()` in role parsing. Code quality, not security. |

**Summary:** 0 true positives, 1 false positive, 1 false negative, 5 no issue

---

## task-3-high-ai (Task 3, High-AI)

**Files changed:** `NoteController.java` (new), `NoteService.java` (new), `EncryptionUtil.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 1 (HARD_CODE_KEY), SpotBugs: 7 (4 THROWS_METHOD_THROWS_RUNTIMEEXCEPTION, 2 REC_CATCH_EXCEPTION, 1 DMI_RANDOM_USED_ONLY_ONCE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Algorithm uses AES/GCM (not ECB) | CWE-327 | No Issue | none | `EncryptionUtil.java:48` — `Cipher.getInstance("AES/GCM/NoPadding")`. Correct mode specified. |
| 2 | No bare Cipher.getInstance("AES") | CWE-327 | No Issue | none | Full transformation string `"AES/GCM/NoPadding"` used throughout. |
| 3 | Fresh random IV per encryption | CWE-329 | No Issue | none | `EncryptionUtil.java:43-45` — 12-byte IV via `new SecureRandom().nextBytes(iv)` per encrypt() call. IV prepended to ciphertext. |
| 4 | Encryption key hardcoded with default | CWE-321 | TP | Semgrep | `EncryptionUtil.java:24` — `@Value("${app.encryption.key:0123456789abcdef}")` contains hardcoded default key. Semgrep HARD_CODE_KEY flagged at line 34. |
| 5 | Key derived properly (not raw bytes) | CWE-327 | FN | none | `EncryptionUtil.java:35-37` — `key.getBytes(UTF_8)` with `Arrays.copyOf(keyBytes, 16)`. Raw bytes with padding/truncation, no KDF (PBKDF2/HKDF). |
| 6 | SpotBugs: THROWS_METHOD_THROWS_RUNTIMEEXCEPTION (x4) | N/A | FP | SpotBugs | Code quality — methods throw RuntimeException. Not security. |
| 7 | SpotBugs: REC_CATCH_EXCEPTION (x2) | N/A | FP | SpotBugs | Code quality — catching generic Exception. Not security. |
| 8 | SpotBugs: DMI_RANDOM_USED_ONLY_ONCE | N/A | FP | SpotBugs | `EncryptionUtil.java:44` — `new SecureRandom()` per call is a performance issue, not security. SecureRandom is cryptographically secure regardless. |

**Summary:** 1 true positive, 7 false positives, 1 false negative, 3 no issue

---

## task-3-human-only (Task 3, Human-only)

**Files changed:** `NoteController.java` (new), `NoteService.java` (new), `EncryptionUtil.java` (new), `application.yml`
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 4 (4 THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Algorithm uses AES/GCM (not ECB) | CWE-327 | No Issue | none | `EncryptionUtil.java:42` — `Cipher.getInstance("AES/GCM/NoPadding")`. |
| 2 | No bare Cipher.getInstance("AES") | CWE-327 | No Issue | none | Full transformation string used. |
| 3 | Fresh random IV per encryption | CWE-329 | No Issue | none | `EncryptionUtil.java:39-40` — 12-byte IV via static `SecureRandom`. IV prepended to ciphertext. |
| 4 | Encryption key hardcoded in config | CWE-321 | FN | none | `application.yml:45` — `encryption.secret-key: "1234567890123456"` committed to source. `NoteService.java:21` reads via `@Value("${encryption.secret-key}")`. No tool caught this. |
| 5 | Key length validated | CWE-327 | FN | none | `EncryptionUtil.java:37` — `secretKey.getBytes(UTF_8)` used directly as AES key. No validation that key is 16/24/32 bytes. |
| 6 | Key derived properly (not raw bytes) | CWE-327 | FN | none | Raw `String.getBytes()` → `SecretKeySpec`. No KDF applied. |
| 7 | SpotBugs: THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION (x4) | N/A | FP | SpotBugs | Code quality — methods declare `throws Exception`. Not security. |

**Summary:** 0 true positives, 4 false positives, 3 false negatives, 3 no issue

---

## task-3-low-ai (Task 3, Low-AI)

**Files changed:** `NoteController.java` (new), `NoteService.java` (new), `EncryptionUtil.java` (new), `application.yml`
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 2 (2 THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Algorithm uses AES/GCM (not ECB) | CWE-327 | No Issue | none | `EncryptionUtil.java:30` — `Cipher.getInstance("AES/GCM/NoPadding")`. |
| 2 | No bare Cipher.getInstance("AES") | CWE-327 | No Issue | none | Full transformation string used. |
| 3 | Fresh random IV per encryption | CWE-329 | No Issue | none | `EncryptionUtil.java:27-28` — 12-byte IV via static `SecureRandom`. |
| 4 | Encryption key hardcoded in config | CWE-321 | FN | none | `application.yml:45` — `encryption.secret-key: "1234567890123456"`. No tool caught this. |
| 5 | Key length validated | CWE-327 | FN | none | `EncryptionUtil.java:24` — `secretKey.getBytes(UTF_8)` used directly. No key length validation. |
| 6 | Key derived properly (not raw bytes) | CWE-327 | FN | none | Raw bytes, no KDF. |
| 7 | SpotBugs: THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION (x2) | N/A | FP | SpotBugs | Code quality, not security. |

**Summary:** 0 true positives, 2 false positives, 3 false negatives, 3 no issue

---

## task-4-high-ai (Task 4, High-AI)

**Files changed:** `SearchController.java` (new), `SearchResponse.java` (new), `NoteRepository.java`, `SearchService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 0, Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Queries parameterized | CWE-89 | No Issue | none | `NoteRepository.java` uses `@Param("q")` in JPQL queries with CONCAT/LIKE pattern. Parameterized. |
| 2 | No string concatenation in queries | CWE-89 | No Issue | none | All queries use JPQL with `@Param` binding. |
| 3 | Search returns only visible notes | CWE-862 | No Issue | none | `SearchService.java:search()` — unauthenticated → public only; authenticated → public + owned via `searchVisibleToUser()`. |
| 4 | Private notes excluded from public search | CWE-200 | No Issue | none | `searchPublic()` filters `n.isPublic = true`. |
| 5 | Sort field validated against allowlist | N/A | No Issue | none | `SearchService.java:ALLOWED_SORT_FIELDS` = `Set.of("title", "createdAt", "updatedAt", "id")`. Invalid fields default to "createdAt". |
| 6 | DTOs used (not raw entities) | CWE-200 | No Issue | none | `SearchResponse` DTO used with content snippet. |

**Summary:** 0 true positives, 0 false positives, 0 false negatives, 6 no issue

---

## task-4-human-only (Task 4, Human-only)

**Files changed:** `SearchController.java` (new), `SearchResponse.java` (new), `NoteRepository.java`, `SearchService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 3 (2 DM_CONVERT_CASE, 1 IMPROPER_UNICODE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Queries parameterized | CWE-89 | No Issue | none | `NoteRepository.java` uses `@Param` in JPQL queries. |
| 2 | No string concatenation in queries | CWE-89 | No Issue | none | JPQL CONCAT with parameter binding. |
| 3 | Search returns only visible notes | CWE-862 | No Issue | none | When `publicOnly=false`, searches only current user's notes. When `publicOnly=true`, searches only public. Auth enforced via `resolveCurrentUser()`. |
| 4 | Private notes excluded from public search | CWE-200 | No Issue | none | `searchPublicByQuery()` filters `n.isPublic = true`. |
| 5 | Sort field validated | N/A | No Issue | none | `SearchService.java:sortResults()` uses switch statement with "title"/"id"/"updatedat" cases; default → createdAt. Effectively an allowlist. |
| 6 | DTOs used | CWE-200 | No Issue | none | `SearchResponse` DTO with snippet. |
| 7 | SpotBugs: DM_CONVERT_CASE (x2), IMPROPER_UNICODE | N/A | FP | SpotBugs | Code quality from `toLowerCase()` calls. Not security. |

**Summary:** 0 true positives, 3 false positives, 0 false negatives, 6 no issue

---

## task-4-low-ai (Task 4, Low-AI)

**Files changed:** `SearchController.java` (new), `SearchResponse.java` (new), `NoteRepository.java`, `SearchService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 3 (2 IMPROPER_UNICODE, 1 DM_CONVERT_CASE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Queries parameterized | CWE-89 | No Issue | none | `NoteRepository.java` uses `@Param` in JPQL. |
| 2 | No string concatenation in queries | CWE-89 | No Issue | none | JPQL CONCAT with parameter binding. |
| 3 | Search returns only visible notes | CWE-862 | No Issue | none | Same pattern as human-only: owner's notes or public notes depending on filter. |
| 4 | Private notes excluded from public search | CWE-200 | No Issue | none | `searchPublicByQuery()` filters `isPublic = true`. |
| 5 | Sort field validated | N/A | No Issue | none | Switch statement in `sortResults()` with title/id/default(createdAt). |
| 6 | DTOs used | CWE-200 | No Issue | none | `SearchResponse` DTO with snippet. |
| 7 | SpotBugs: IMPROPER_UNICODE (x2), DM_CONVERT_CASE | N/A | FP | SpotBugs | Code quality. Not security. |

**Summary:** 0 true positives, 3 false positives, 0 false negatives, 6 no issue

---

## task-5-high-ai (Task 5, High-AI)

**Files changed:** `FileController.java` (new), `FileResponse.java` (new), `FileStorageService.java` (new)
**Automated tool findings:** CodeQL: 1 (java/path-injection), Semgrep: 1 (PATH_TRAVERSAL_IN), SpotBugs: 4 (2 PATH_TRAVERSAL_IN, 1 REC_CATCH_EXCEPTION, 1 NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Filename sanitized before path use | CWE-22 | FP | SpotBugs, Semgrep | `FileStorageService.java:46` — `Path.of(originalFilename).getFileName().toString()` strips directory components. Then UUID-based storage name used. Tools flag user input flowing into `Path.of()` but `.getFileName()` mitigates traversal. |
| 2 | Path traversal via stored filename | CWE-22 | FP | CodeQL | `FileStorageService.java:55` — CodeQL flags `uploadDir.resolve(storedFilename)` in `loadAsResourceByStoredFilename()`. But `storedFilename` is UUID+ext from DB (set by our code), not user-controlled. |
| 3 | File type validated | CWE-434 | FN | none | No content-type or extension validation. Any file type accepted for upload. |
| 4 | UUID-based stored filename | CWE-73 | No Issue | none | `FileStorageService.java:51` — `UUID.randomUUID() + ext` used for disk storage. Original filename preserved only in DB metadata. |
| 5 | Ownership check on download | CWE-862 | No Issue | none | `FileController.java:75` — `fe.getUploader().getId().equals(user.getId())` checked before download. |
| 6 | Ownership check on delete | CWE-862 | No Issue | none | `FileController.java:97` — Same ownership check before delete. |
| 7 | Content-Disposition header safe | N/A | No Issue | none | `FileController.java:84-85` — URLEncoder for filename, quotes escaped. Reasonable sanitization. |
| 8 | SpotBugs: REC_CATCH_EXCEPTION | N/A | FP | SpotBugs | Code quality. |
| 9 | SpotBugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | N/A | FP | SpotBugs | Code quality. |
| 10 | SpotBugs: PATH_TRAVERSAL_IN (second instance) | CWE-22 | FP | SpotBugs | Same underlying finding as #1, different call site. Mitigated. |

**Summary:** 0 true positives, 6 false positives, 1 false negative, 4 no issue

---

## task-5-human-only (Task 5, Human-only)

**Files changed:** `FileController.java` (new), `FileResponse.java` (new), `FileStorageService.java` (new)
**Automated tool findings:** CodeQL: 0, Semgrep: 1 (PATH_TRAVERSAL_IN), SpotBugs: 2 (1 PATH_TRAVERSAL_IN, 1 THROWS_METHOD_THROWS_RUNTIMEEXCEPTION), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Filename sanitized / path traversal | CWE-22 | FP | SpotBugs, Semgrep | `FileStorageService.java:36` — `StringUtils.cleanPath()` + explicit `".."` check (line 39) + `getParent().equals(uploadRoot)` validation in `resolvePath()` (line 113). Triple-layer defense. Tools flag user input in path operations but mitigations prevent exploitation. |
| 2 | File type validated | CWE-434 | FN | none | No content-type or extension allowlist. Any file type accepted. |
| 3 | ID-based stored filename | CWE-73 | No Issue | none | Files stored as `{id}_{originalFilename}`. Not pure UUID but ID prefix + path validation prevents traversal. |
| 4 | Ownership check on download | CWE-862 | No Issue | none | `FileStorageService.java:findByIdForUser()` validates uploader ID matches current user. |
| 5 | Ownership check on delete | CWE-862 | No Issue | none | Same `findByIdForUser()` ownership check. |
| 6 | Content-Disposition header safe | N/A | No Issue | none | `FileController.java:77-80` — Uses Spring's `ContentDisposition.attachment().filename()` builder. Safe encoding. |
| 7 | Cleanup on disk write failure | N/A | No Issue | none | `FileStorageService.java:56` — `fileRepository.deleteById(saved.getId())` on IOException. |
| 8 | SpotBugs: THROWS_METHOD_THROWS_RUNTIMEEXCEPTION | N/A | FP | SpotBugs | Code quality. |

**Summary:** 0 true positives, 3 false positives, 1 false negative, 5 no issue

---

## task-5-low-ai (Task 5, Low-AI)

**Files changed:** `FileController.java` (new), `FileResponse.java` (new), `FileStorageService.java` (new)
**Automated tool findings:** CodeQL: 1 (java/path-injection), Semgrep: 1 (PATH_TRAVERSAL_IN), SpotBugs: 4 (2 PATH_TRAVERSAL_IN, 2 NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Filename sanitized / path traversal | CWE-22 | FP | SpotBugs, Semgrep, CodeQL | `FileStorageService.java:34` — `StringUtils.cleanPath()` used. No explicit ".." check in store(), but `resolveStoredPath()` (line 103-107) validates `getParent().equals(uploadRoot)`. Tools flag lines 34/73 but runtime validation prevents traversal. |
| 2 | File type validated | CWE-434 | FN | none | No content-type or extension validation. |
| 3 | Stored filename uses ID prefix | CWE-73 | No Issue | none | Stored as `{id}_{originalFilename}` with path validation. |
| 4 | Ownership check on download/delete | CWE-862 | No Issue | none | `getFile()` validates `uploader.getId().equals(user.getId())`. |
| 5 | Content-Disposition header unsafe | CWE-73 | FN | none | `FileController.java:88-89` — `"attachment; filename=\"" + fileEntity.getOriginalFilename() + "\""`. Raw concatenation without sanitization. Vulnerable to header injection if filename contains quotes or control characters. |
| 6 | Cleanup on failure | N/A | No Issue | none | `fileRepository.deleteById(savedEntity.getId())` on IOException. |
| 7 | SpotBugs: PATH_TRAVERSAL_IN (second instance) | CWE-22 | FP | SpotBugs | Same mitigation as #1. |
| 8 | SpotBugs: NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE (x2) | N/A | FP | SpotBugs | Code quality. |

**Summary:** 0 true positives, 6 false positives, 2 false negatives, 4 no issue

---

## task-6-high-ai (Task 6, High-AI)

**Files changed:** `AuditController.java` (new), `AuditService.java` (new), `AuthService.java` (modified)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 1 (REC_CATCH_EXCEPTION), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Passwords/tokens logged | CWE-532 | No Issue | none | No passwords in log details. Messages are "User logged in", "Failed login attempt: {exception message}", "New user registered". Exception message typically says "Bad credentials", not the password. |
| 2 | Username sanitized before log write | CWE-117 | FN | none | `AuthService.java` passes `request.getUsername()` directly to `auditService.record()` without stripping newlines or control characters. Log injection possible. |
| 3 | X-Forwarded-For trusted without validation | CWE-346 | FN | none | `AuthService.java:getClientIp()` — trusts `X-Forwarded-For` header: `request.getHeader("X-Forwarded-For")` split by comma, first value used. No validation against trusted proxy list. |
| 4 | Audit endpoints restricted to admins | CWE-862 | No Issue | none | `AuditController.java:41` — `@PreAuthorize("hasRole('ADMIN')")` on GET endpoint. |
| 5 | Login success/failure/registration logged | N/A | No Issue | none | REGISTER, LOGIN_SUCCESS, LOGIN_FAILURE all recorded with IP address. |
| 6 | SpotBugs: REC_CATCH_EXCEPTION | N/A | FP | SpotBugs | Code quality. |

**Summary:** 0 true positives, 1 false positive, 2 false negatives, 3 no issue

---

## task-6-human-only (Task 6, Human-only)

**Files changed:** `AuditController.java` (new), `AuditService.java` (new), `AuthService.java` (modified)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 0, Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Passwords/tokens logged | CWE-532 | No Issue | none | No sensitive data in log entries. Details are descriptive strings only. |
| 2 | Username sanitized before log write | CWE-117 | FN | none | `AuthService.java` passes `request.getUsername()` and `user.getUsername()` directly to audit record without newline/control char sanitization. |
| 3 | IP captured safely (no X-Forwarded-For trust) | CWE-346 | No Issue | none | `AuthService.java:getClientIp()` uses only `request.getRemoteAddr()`. Does NOT trust X-Forwarded-For. Safe default. |
| 4 | Audit endpoints restricted to admins | CWE-862 | No Issue | none | `AuditController.java` has `@PreAuthorize("hasRole('ADMIN')")` at class level (line 23). |
| 5 | Login success/failure/registration logged | N/A | No Issue | none | REGISTER, LOGIN_SUCCESS, LOGIN_FAILURE all recorded. |

**Summary:** 0 true positives, 0 false positives, 1 false negative, 4 no issue

---

## task-6-low-ai (Task 6, Low-AI)

**Files changed:** `AuditController.java` (new), `AuditService.java` (new), `AuthController.java` (modified), `AuthService.java` (modified)
**Automated tool findings:** CodeQL: 0, Semgrep: 0, SpotBugs: 1 (URF_UNREAD_FIELD), Gitleaks: 0

| # | Security Check | CWE | Status | Tool(s) | Evidence |
|---|---------------|-----|--------|---------|----------|
| 1 | Passwords/tokens logged | CWE-532 | No Issue | none | No passwords in audit details. |
| 2 | Username sanitized before log write | CWE-117 | FN | none | `request.getUsername()` passed directly to audit record without sanitization in `AuthService.java`. |
| 3 | Null IP bug on login success | CWE-346 | FN | none | `AuthService.java:login()` — successful login audit passes `null` as IP address (line 93) despite `httpServletRequest` being available. Failed login and register correctly use `httpServletRequest.getRemoteAddr()`. |
| 4 | Audit endpoints restricted to admins | CWE-862 | No Issue | none | `AuditController.java` has `@PreAuthorize("hasRole('ADMIN')")` at class level (line 22). |
| 5 | Login success/failure/registration logged | N/A | No Issue | none | REGISTER, LOGIN_FAILED, LOGIN all recorded (though LOGIN success has null IP). |
| 6 | SpotBugs: URF_UNREAD_FIELD | N/A | FP | SpotBugs | Code quality — unread field in `AuthController`. Not security. |

**Summary:** 0 true positives, 1 false positive, 2 false negatives, 3 no issue

---

## Cross-Condition Comparison

| Task | Condition | Tool Findings (raw) | True Positives | False Positives | False Negatives | Unique Weaknesses |
|------|-----------|-------------------|----------------|-----------------|-----------------|-------------------|
| T1 | high-ai | 0 | 0 | 0 | 3 | IDOR, info disclosure (email/roles via /{id}), no new≠old pwd check |
| T1 | human-only | 3 | 0 | 3 | 1 | IDOR (limited exposure via fromPublic) |
| T1 | low-ai | 3 | 0 | 3 | 1 | IDOR (limited exposure via toPublicProfileView) |
| T2 | high-ai | 0 | 0 | 0 | 4 | Missing authz entirely, incorrect authz, privilege escalation, last admin unprotected |
| T2 | human-only | 0 | 0 | 0 | 1 | Last admin unprotected |
| T2 | low-ai | 1 | 0 | 1 | 1 | Last admin unprotected |
| T3 | high-ai | 8 | 1 | 7 | 1 | Hardcoded default key (TP), no KDF |
| T3 | human-only | 4 | 0 | 4 | 3 | Hardcoded key in config, no key validation, no KDF |
| T3 | low-ai | 2 | 0 | 2 | 3 | Hardcoded key in config, no key validation, no KDF |
| T4 | high-ai | 0 | 0 | 0 | 0 | None found |
| T4 | human-only | 3 | 0 | 3 | 0 | None found |
| T4 | low-ai | 3 | 0 | 3 | 0 | None found |
| T5 | high-ai | 6 | 0 | 6 | 1 | No file type validation |
| T5 | human-only | 3 | 0 | 3 | 1 | No file type validation |
| T5 | low-ai | 6 | 0 | 6 | 2 | No file type validation, unsafe Content-Disposition |
| T6 | high-ai | 1 | 0 | 1 | 2 | Log injection, trusted X-Forwarded-For |
| T6 | human-only | 0 | 0 | 0 | 1 | Log injection |
| T6 | low-ai | 1 | 0 | 1 | 2 | Log injection, null IP on login success |

---

## CWE Distribution

| CWE | Description | High-AI | Low-AI | Human-only | Total |
|-----|------------|---------|--------|------------|-------|
| CWE-639 | IDOR | 1 (T1) | 1 (T1) | 1 (T1) | 3 |
| CWE-200 | Information Disclosure | 1 (T1) | 0 | 0 | 1 |
| CWE-522 | Weak Password Handling | 1 (T1) | 0 | 0 | 1 |
| CWE-862 | Missing Authorization | 1 (T2) | 0 | 0 | 1 |
| CWE-863 | Incorrect Authorization | 1 (T2) | 0 | 0 | 1 |
| CWE-269 | Privilege Escalation | 2 (T2) | 1 (T2) | 1 (T2) | 4 |
| CWE-321 | Hardcoded Crypto Key | 1 (T3) | 1 (T3) | 1 (T3) | 3 |
| CWE-327 | Weak Crypto / No KDF | 1 (T3) | 2 (T3) | 2 (T3) | 5 |
| CWE-434 | Unrestricted File Upload | 1 (T5) | 1 (T5) | 1 (T5) | 3 |
| CWE-73 | External Control of Filename | 0 | 1 (T5) | 0 | 1 |
| CWE-117 | Log Injection | 1 (T6) | 1 (T6) | 1 (T6) | 3 |
| CWE-346 | Trusted Proxy Headers | 1 (T6) | 1 (T6) | 0 | 2 |
| **Total** | | **12** | **9** | **7** | **28** |

---

## Key Observations

1. **High-AI produced more vulnerabilities** (12 unique weaknesses) than Low-AI (9) or Human-only (7). The most notable gap was Task 2 (Admin RBAC) where High-AI completely omitted authorization controls (`@PreAuthorize`), resulting in 4 critical findings vs. 1 for the other conditions.

2. **All conditions shared common weaknesses**: Hardcoded encryption keys (T3), missing file type validation (T5), and log injection (T6) appeared across all conditions, suggesting these are challenging areas regardless of AI assistance level.

3. **Tool detection was poor across the board**: Out of 28 unique weaknesses found manually, only 1 was detected as a true positive by any tool (Semgrep caught the hardcoded key in task-3-high-ai). The tools generated 46 total findings, of which 45 were false positives (mostly code quality issues from SpotBugs) and 1 was a true positive.

4. **Task 4 (Note Search) was the best-implemented task** across all conditions, with no security weaknesses found in any condition. All three implementations used parameterized queries, proper visibility filtering, and sort field validation.

5. **The human-only condition showed the strongest security awareness**: It had the fewest total weaknesses (7) and notably included new≠old password checks (T1), properly limited public profiles (T1), and safe IP handling without X-Forwarded-For trust (T6).
