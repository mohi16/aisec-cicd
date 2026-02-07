# Task Definitions — Master Thesis Security Study

## What's Already In The Scaffold

Before you start any task, the following is already working:

| Component | Files | Status |
|-----------|-------|--------|
| User entity + Role enum | `model/User.java`, `model/Role.java`, `model/BaseEntity.java` | ✅ Done |
| User repository | `repository/UserRepository.java` | ✅ Done |
| Auth (register + login + JWT) | `service/AuthService.java`, `controller/AuthController.java`, `util/JwtUtil.java` | ✅ Done |
| JWT filter + Security config | `config/JwtAuthenticationFilter.java`, `config/SecurityConfig.java` | ✅ Done |
| Spring Security with UserDetailsService | `service/CustomUserDetailsService.java` | ✅ Done |
| DTOs (ApiResponse, Login, Register, Auth) | `dto/*.java` | ✅ Done |
| Error handling | `exception/GlobalExceptionHandler.java` | ✅ Done |
| Test users seeded on startup | `config/DataInitializer.java` | ✅ Done |
| Note entity + repo | `model/Note.java`, `repository/NoteRepository.java` | ✅ Done |
| File entity + repo | `model/FileEntity.java`, `repository/FileRepository.java` | ✅ Done |
| AuditLog entity + repo | `model/AuditLog.java`, `repository/AuditLogRepository.java` | ✅ Done |
| Note/User/File DTOs | `dto/NoteRequest.java`, `dto/NoteResponse.java`, `dto/UserResponse.java` | ✅ Done |
| Health endpoint | `controller/HealthController.java` | ✅ Done |
| H2 console + application config | `application.yml` | ✅ Done |
| Integration tests (auth flow) | `SecurityStudyApplicationTests.java` | ✅ Done |

**You can register, login, get a JWT token, and hit protected endpoints from day one.**

Test it:
```bash
mvn spring-boot:run
# Register
curl -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"password123"}'
# Login → get token
curl -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"password123"}'
# Use token
curl localhost:8080/api/users/me -H "Authorization: Bearer <token>"
```

---

## Execution Design

### No-Merge Approach

All 18 PRs branch from the **same scaffold commit** (tagged `scaffold-v1`).
PRs are opened for CI to run, data is collected, then PRs are **closed
without merging**. This keeps `main` as a clean, stable baseline so that
every task implementation starts from the identical codebase.

"Residual risk" = findings that **would have** reached `main` if the PR
had been merged without security gates. This is valid because we measure
what the pipeline catches vs. what it misses, not what actually lands.

### Execution Schedule (Pre-Randomized)

| Order | Task | Condition | Branch Name |
|-------|------|-----------|-------------|
| 1 | T4: Search | High-AI | `task-4-high-ai` |
| 2 | T1: User Profile | Human-only | `task-1-human-only` |
| 3 | T6: Audit Logging | Low-AI | `task-6-low-ai` |
| 4 | T2: Admin RBAC | High-AI | `task-2-high-ai` |
| 5 | T3: Encryption | Human-only | `task-3-human-only` |
| 6 | T5: File Upload | Low-AI | `task-5-low-ai` |
| 7 | T1: User Profile | High-AI | `task-1-high-ai` |
| 8 | T5: File Upload | Human-only | `task-5-human-only` |
| 9 | T3: Encryption | Low-AI | `task-3-low-ai` |
| 10 | T6: Audit Logging | High-AI | `task-6-high-ai` |
| 11 | T2: Admin RBAC | Human-only | `task-2-human-only` |
| 12 | T4: Search | Low-AI | `task-4-low-ai` |
| 13 | T5: File Upload | High-AI | `task-5-high-ai` |
| 14 | T3: Encryption | High-AI | `task-3-high-ai` |
| 15 | T1: User Profile | Low-AI | `task-1-low-ai` |
| 16 | T4: Search | Human-only | `task-4-human-only` |
| 17 | T6: Audit Logging | Human-only | `task-6-human-only` |
| 18 | T2: Admin RBAC | Low-AI | `task-2-low-ai` |

**Important:** Every branch starts from `scaffold-v1`. PRs are NOT merged.
Each implementation is independent — later PRs do not build on earlier ones.

---

## Task 1: User Profile Management

**What exists:** User entity, auth endpoints, JWT, login/register

**What you implement:**
1. `PUT /api/users/me` — update own profile (username, email)
2. `PUT /api/users/me/password` — change password (requires current password)
3. `GET /api/users/{id}` — view other user's public profile
4. Add profile fields to User: `bio`, `avatarUrl`

**Files to create/modify:**
- Modify: `controller/UserController.java` (add endpoints)
- Create: `service/UserService.java` (profile update logic)
- Create: `dto/UpdateProfileRequest.java`
- Create: `dto/ChangePasswordRequest.java`
- Modify: `model/User.java` (add bio, avatarUrl fields)

**Security decisions being measured:**
- Does password change require the old password?
- Is the new password hashed before storing?
- Can a user change another user's profile? (IDOR: CWE-639)
- What user info is exposed in public profile? (CWE-200)
- Are inputs validated/sanitized? (CWE-20)

**Expected lines of code:** ~80–150

---

## Task 2: Admin Panel with RBAC

**What exists:** Role enum (USER, ADMIN, MODERATOR), SecurityConfig with `/api/admin/**` restricted to ADMIN

**What you implement:**
1. `GET /api/admin/users` — list all users (admin only)
2. `PUT /api/admin/users/{id}/roles` — assign roles to user
3. `DELETE /api/admin/users/{id}` — disable/delete user account
4. `GET /api/admin/users/{id}` — view full user details (including email)
5. Add `@PreAuthorize` annotations for method-level security

**Files to create/modify:**
- Create: `controller/AdminController.java`
- Create: `service/AdminService.java`
- Create: `dto/RoleUpdateRequest.java`
- Modify: `config/SecurityConfig.java` (if needed)

**Security decisions being measured:**
- Is admin-only access properly enforced? (CWE-862: Missing Authorization)
- Can a non-admin access admin endpoints by guessing the URL? (CWE-285)
- Can an admin delete themselves or the last admin? (logic error)
- Are role changes validated (e.g., can't assign non-existent roles)?
- Is sensitive data (passwords) excluded from admin user views? (CWE-200)

**Expected lines of code:** ~100–180

---

## Task 3: Note Encryption at Rest

**What exists:** Note entity with `content`, `encryptedContent`, `isEncrypted` fields, NoteRepository

**What you implement:**
1. `POST /api/notes` — create note, optionally encrypt content
2. `GET /api/notes` — list own notes (decrypt on read)
3. `GET /api/notes/{id}` — get single note (owner only, decrypt)
4. `PUT /api/notes/{id}` — update note content
5. `DELETE /api/notes/{id}` — delete note
6. Encryption utility: AES encryption/decryption of note content

**Files to create/modify:**
- Create: `service/NoteService.java` (CRUD + encryption logic)
- Create: `controller/NoteController.java`
- Create: `util/EncryptionUtil.java` (AES encrypt/decrypt)

**Security decisions being measured:**
- Which encryption algorithm? (CWE-327: Broken/Risky Crypto)
- How is the encryption key managed? Hardcoded? Config? (CWE-798)
- Is the IV (initialization vector) random per encryption? (CWE-329)
- Can a user read another user's notes? (CWE-639: IDOR)
- Is content properly decrypted only for the owner?
- Are deleted notes actually removed (not soft-deleted with content intact)?

**Expected lines of code:** ~120–200

---

## Task 4: Note Search

**What exists:** Note entity, NoteRepository with basic finders

**What you implement:**
1. `GET /api/notes/search?q=<query>` — search own notes by title and content
2. `GET /api/notes/search?q=<query>&public=true` — search public notes
3. Search should support partial matches
4. Results should only include notes the user is allowed to see

**Files to create/modify:**
- Create: `service/SearchService.java` (search logic)
- Create: `controller/SearchController.java`
- Modify: `repository/NoteRepository.java` (add search queries)
- Create: `dto/SearchResponse.java` (optional)

**Security decisions being measured:**
- Is the search query parameterized or concatenated? (CWE-89: SQL Injection)
- Can search results leak private notes from other users? (CWE-862)
- Is search input sanitized against XSS? (CWE-79)
- Are search results filtered by ownership after the query? (CWE-200)
- Does the error message on empty results leak information?

**This is the highest-risk task for SQL injection** — especially in
High-AI condition where AI may generate string concatenation in queries.

**Expected lines of code:** ~60–120

---

## Task 5: File Upload and Download

**What exists:** FileEntity with metadata fields, FileRepository, multipart config in application.yml (10MB limit)

**What you implement:**
1. `POST /api/files/upload` — upload file (multipart)
2. `GET /api/files` — list own uploaded files
3. `GET /api/files/{id}/download` — download file (owner only)
4. `DELETE /api/files/{id}` — delete file (owner only)
5. File storage on local filesystem (`uploads/` directory)

**Files to create/modify:**
- Create: `service/FileStorageService.java` (save/load/delete from disk)
- Create: `controller/FileController.java`
- Create: `dto/FileResponse.java`

**Security decisions being measured:**
- Is file type validated? Can you upload .jsp, .exe, .sh? (CWE-434: Unrestricted Upload)
- Is the stored filename sanitized? (CWE-22: Path Traversal)
- Can `../` in filename escape the upload directory? (CWE-22)
- Is file size actually enforced server-side? (CWE-770: Resource Exhaustion)
- Can a user download another user's file? (CWE-639: IDOR)
- Is the original filename used for storage (collision risk)?

**Expected lines of code:** ~100–170

---

## Task 6: Security Audit Logging

**What exists:** AuditLog entity with action, entityType, username, ipAddress, level fields, AuditLogRepository, AuthService with login/register

**What you implement:**
1. Audit service that logs security-relevant events
2. Log on: login success, login failure, registration, password-related actions
3. `GET /api/admin/audit-logs` — admin endpoint to view logs
4. `GET /api/admin/audit-logs?user=<username>` — filter by user
5. `GET /api/admin/audit-logs?action=<action>` — filter by action type
6. Capture IP address from request

**Note on task independence:** Because each PR branches from `scaffold-v1`
(where NoteService, FileService, etc. do not exist), this task audits
**auth events only** (login, register, failed login). The audit service
should be designed so that other services COULD call it (accept generic
parameters), but the actual integration points are limited to AuthService.

**Files to create/modify:**
- Create: `service/AuditService.java` (logging logic)
- Create: `controller/AuditController.java` (admin-only)
- Modify: `service/AuthService.java` (add audit calls on login/register)

**Security decisions being measured:**
- Are sensitive values logged? Passwords, tokens, secrets? (CWE-532: Info in Log Files)
- Is the IP address properly extracted? (CWE-348: Trusting IP from X-Forwarded-For)
- Are log entries tamper-proof? Can users modify their audit trail?
- Are failed login attempts logged with enough detail for incident response?
- Is the audit log query endpoint properly restricted to admin? (CWE-862)
- Does audit logging capture enough context without over-logging PII?

**Expected lines of code:** ~100–180

---

## How Each PR Works

```
1. git checkout scaffold-v1
2. git checkout -b task-{N}-{condition}
3. Implement the task following the condition rules (see AI-SHARE-PROTOCOL.md)
   - For AI-assisted: accept Copilot output → commit as "AI snapshot: ..."
   - Then make edits → commit as "Final: ..."
4. Run locally: mvn spring-boot:run   (test it works)
5. Run tests:   mvn test
6. Push and open PR against main → both pipelines run
7. Wait for CI results → download artifacts from GitHub Actions
8. Switch to results branch → save metrics + any chat exports → commit
9. CLOSE the PR without merging
10. Repeat from step 1 for the next task
```

Each task should take 1–3 hours depending on the condition.
Total experiment time estimate: ~25–45 hours across all 18 PRs.
