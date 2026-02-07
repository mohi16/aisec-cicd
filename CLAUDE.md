# CLAUDE.md — Project Context for Claude Code

## What This Is

This is the codebase for a **master thesis** at FH Technikum Wien. The thesis studies how AI-assisted code generation affects software security in CI/CD pipelines.

## The Experiment

One developer (the student) implements **6 feature tasks**, each under **3 AI conditions** (Human-only, Low-AI, High-AI) = **18 pull requests** total. Every PR branches from the same scaffold commit (tagged `scaffold-v1`). PRs are opened for CI, data is collected, then PRs are **closed without merging**. This ensures every implementation starts from the same codebase. Each PR runs through two pipelines:

- **Baseline pipeline**: format check, compile, unit tests (no security scanning)
- **Security-gated pipeline**: same + CodeQL, Semgrep, Gitleaks, OWASP Dependency-Check

The key metric is **AI Share** (0.0–1.0): the proportion of code in each PR that originated from AI assistance, computed by comparing stored AI patches against the final diff.

## Research Questions

- **RQ1**: In a Baseline pipeline, how does residual CWE risk change as AI Share increases?
- **RQ2**: How effective is the Security-gated pipeline at reducing risk, and what cost (CI duration, failure rate) does it impose?
- **RQ3**: Do CWE types/severity shift with AI Share, and does the gate disproportionately delay high-AI PRs?

## Tech Stack

- Java 21, Spring Boot 3.4.1, Maven
- H2 in-memory database (dev), Spring Security with JWT
- GitHub Actions for CI/CD
- Security tools: CodeQL, Semgrep, Gitleaks, OWASP Dependency-Check
- Analysis tools: Python (pandas, matplotlib, seaborn, scipy)

## Project Structure

```
src/main/java/com/thesis/securitystudy/
├── config/          # SecurityConfig, JwtAuthFilter, DataInitializer
├── controller/      # AuthController, UserController, HealthController
├── dto/             # Request/Response DTOs (ApiResponse, LoginRequest, etc.)
├── exception/       # GlobalExceptionHandler, ResourceNotFoundException
├── model/           # User, Role, Note, FileEntity, AuditLog, BaseEntity
├── repository/      # JPA repositories for all entities
├── service/         # AuthService, CustomUserDetailsService
├── util/            # JwtUtil
.github/workflows/
├── baseline.yml         # Baseline pipeline (no security)
├── security-gated.yml   # Security-gated pipeline (4 tools)
docs/
├── TASK-DEFINITIONS.md  # What to build for each of the 6 tasks
├── AI-SHARE-PROTOCOL.md # How AI Share is measured
├── FINDING-SCHEMA.md    # How security findings are recorded/deduplicated
├── MANUAL-REVIEW-CHECKLIST.md
tools/
├── compute_ai_share.py  # Computes AI Share from patches + diff
├── parse_findings.py    # Normalizes SARIF/JSON from all 4 tools
├── collect_metrics.py   # Pulls CI run data from GitHub Actions API
├── analyze.py           # Statistical analysis + publication-ready figures
```

## What's Already Built (Scaffold)

The scaffold provides shared infrastructure that all tasks build on:
- Full auth flow: register, login, JWT token generation/validation
- User entity with roles (USER, ADMIN, MODERATOR)
- Note, FileEntity, AuditLog entities with repositories
- DTOs for requests/responses
- Global exception handling
- Test users seeded on startup (admin/admin123, testuser/user1234)
- Both CI/CD pipelines configured

## The 6 Tasks (What the Student Implements)

Each task is a feature PR. See `docs/TASK-DEFINITIONS.md` for full details.

1. **T1: User Profile** — PUT /api/users/me, password change, public profiles
2. **T2: Admin RBAC** — Admin panel, role management, user disable
3. **T3: Note Encryption** — CRUD notes with AES encryption at rest
4. **T4: Note Search** — Search with parameterized queries (SQL injection risk)
5. **T5: File Upload** — Multipart upload/download with path traversal risks
6. **T6: Audit Logging** — Security event logging, admin log viewer

## AI Tool

All 12 AI-assisted PRs use **GitHub Copilot** (Ask mode) with **GPT-5 mini** as the model. No other AI tools are used for code generation. No model switching during the experiment.

## AI Conditions (How Each PR Is Done)

- **Human-only**: Copilot DISABLED. Manual coding only. IDE autocomplete OK.
- **Low-AI**: Copilot enabled for boilerplate only. Developer writes core/security logic manually.
- **High-AI**: Copilot is primary author. Developer writes prompts/comments, accepts suggestions aggressively, uses Copilot Chat freely. Minimal manual edits.

## Important Constraints

- This is a **within-subject** experiment: same developer, different AI levels
- Each task is implemented **3 times** (once per condition)
- Execution order is **pre-randomized** (see TASK-DEFINITIONS.md)
- AI patches must be **saved before editing** for AI Share computation
- Security findings are **triaged as TP/FP** by the student after CI runs

## Commands

```bash
mvn spring-boot:run          # Run the app (localhost:8080)
mvn test                     # Run tests
mvn spotless:check           # Check formatting
mvn spotless:apply           # Fix formatting
```

## When Helping (Claude Code / Claude Chat)

Claude is NOT used for the 18 task implementations — that's Copilot's job.
Claude may be used for:
- Debugging pipeline/CI issues
- Running analysis tools (compute_ai_share, parse_findings, analyze)
- Writing the thesis document
- Answering methodology questions
- Fixing infrastructure problems in the scaffold

These uses are NOT captured as AI patches and do NOT affect AI Share.
