# Security Study: AI Share and CI/CD Pipeline Effectiveness

Master thesis research project examining how AI-assisted development affects
software security and CI/CD pipeline performance.

## Research Design

- **6 feature tasks × 3 conditions = 18 pull requests**
- **Conditions:** Human-only, Low-AI, High-AI (protocol-defined, not percentage targets)
- **Pipelines:** Baseline (no security scanning) vs. Security-gated (CodeQL + Semgrep + Gitleaks + OWASP Dependency-Check)
- **Key metric:** AI Share — proportion of code originating from AI assistance (continuous, 0.0–1.0)
- **AI Share is computed from:** stored AI patches + final PR diff (reproducible by third parties)

## Tech Stack

- Java 21 / Spring Boot 3.4.x
- Spring Security, Spring Data JPA
- H2 (dev/test), PostgreSQL (optional)
- GitHub Actions for CI/CD

## Security Tools

| Tool | Purpose | Gate Condition |
|------|---------|----------------|
| CodeQL | SAST (deep semantic analysis) | Any security finding (SARIF parsed, explicit fail) |
| Semgrep | SAST (pattern-based) | ERROR severity findings |
| Gitleaks | Secret detection | Any secret detected |
| OWASP Dependency-Check | SCA (vulnerable dependencies) | CVSS ≥ 7.0 |

## Project Structure

```
├── .github/workflows/
│   ├── baseline.yml              # Baseline pipeline (no security)
│   └── security-gated.yml       # Security-gated pipeline
├── src/main/java/com/thesis/securitystudy/
│   ├── config/                   # Security config, app config
│   ├── controller/               # REST controllers
│   ├── dto/                      # Data transfer objects
│   ├── exception/                # Exception handlers
│   ├── model/                    # JPA entities
│   ├── repository/               # Data access
│   ├── service/                  # Business logic
│   └── util/                     # Utilities (JWT, encryption, etc.)
├── data/
│   ├── README.md                 # Setup instructions for results branch
│   └── .gitignore                # Excludes CSVs from main branch
├── docs/
│   ├── TASK-DEFINITIONS.md       # 6 feature task specifications
│   ├── AI-SHARE-PROTOCOL.md     # Measurement protocol (patch-based)
│   ├── FINDING-SCHEMA.md        # Finding schema + deduplication policy
│   └── MANUAL-REVIEW-CHECKLIST.md
├── .semgrep.yml                  # Semgrep custom rules
└── .gitleaks.toml                # Gitleaks configuration
```

## Important: Data Separation

Measurement data (CSVs, AI patches) lives on a separate `results` branch,
NOT in PR diffs. This prevents distorting AI Share calculations and
triggering false scanner hits. See `data/README.md` for setup.

## Getting Started

```bash
# Build
mvn clean compile

# Run tests
mvn test

# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply

# Run OWASP Dependency-Check locally
mvn dependency-check:check
```

## Data Collection Workflow

1. Pick next task + condition from randomization schedule (see TASK-DEFINITIONS.md)
2. Create feature branch: `git checkout -b task-X-condition`
3. Follow the AI Share protocol for the assigned condition
4. **Capture AI patches** (copy raw AI output before editing) to a scratch folder
5. Open PR against `main` — both pipelines run automatically
6. Export final diff: `git diff main...HEAD > pr-{N}-final.diff`
7. Switch to `results` branch, record pipeline results + AI patches
8. Triage tool findings per FINDING-SCHEMA.md
9. Perform manual review if applicable (all High-AI + all clean PRs + 30-40% sample)
10. Merge PR and repeat
