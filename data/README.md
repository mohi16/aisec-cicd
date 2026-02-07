# Experiment Data (Results Branch)

All measurement data lives on the `results` branch, NOT on `main`.

## Directory Structure

```
results/
├── README.md
├── pr-001/                    # PR #1: T4 Search (High-AI)
│   ├── metadata.json          # PR metadata (task, condition, model, timestamps, SHAs)
│   ├── final.diff             # git diff scaffold-v1..origin/task-4-high-ai
│   ├── artifacts/             # CI outputs
│   │   ├── codeql.sarif
│   │   ├── semgrep.sarif
│   │   ├── semgrep.json
│   │   ├── gitleaks.sarif
│   │   └── dependency-check.json
│   └── copilot-chat/          # (optional) Copilot Chat exports
│       └── chat-001.txt
├── pr-002/                    # PR #2: T1 User Profile (Human-only)
│   ├── metadata.json
│   ├── final.diff
│   └── artifacts/
├── ...
├── pr-018/
├── screenshots/               # IDE config evidence
│   └── copilot-config.png
├── pr-metrics.csv             # Aggregated: AI Share + CI metrics per PR
└── findings.csv               # All triaged security findings
```

## Creating a PR Folder

After CI finishes for a PR:

```bash
git checkout results

# Create folder
mkdir -p pr-001/artifacts pr-001/copilot-chat

# Generate diff from remote branch (works even if local branch is deleted)
git diff scaffold-v1..origin/task-4-high-ai > pr-001/final.diff

# Copy metadata template and fill it in
cp metadata-template.json pr-001/metadata.json
# Edit pr-001/metadata.json with actual values

# Copy downloaded CI artifacts into pr-001/artifacts/

git add pr-001/
git commit -m "PR-001: T4 High-AI data"
git push origin results
git checkout main
```

## metadata.json Template

See `metadata-template.json` in the results branch root.
