# Thesis Tools

Python scripts for data collection and analysis. All tools are standalone
and designed to be run from the repo root.

## Setup

```bash
pip install -r tools/requirements.txt
```

## Tool 1: compute_ai_share.py

Computes AI Share for a PR by comparing stored AI patches against the final diff.

```bash
# After completing a PR, from the results branch:
python tools/compute_ai_share.py \
    --pr 3 \
    --patches-dir data/ai-patches \
    --verbose \
    --output data/ai-share-results.csv
```

**Inputs:** AI patch files (`pr-{N}-*.diff`, `pr-{N}-*.txt`) + final diff (`pr-{N}-final.diff`)
**Output:** AI Share value (0.0–1.0), appended to CSV

## Tool 2: parse_findings.py

Parses security tool outputs (SARIF/JSON) into a unified findings.csv with
fingerprints, deduplication, and cross-tool overlap detection.

```bash
# After downloading pipeline artifacts for a PR:
python tools/parse_findings.py \
    --pr 3 \
    --task T1 \
    --condition high-ai \
    --ai-share 0.82 \
    --pipeline security-gated \
    --codeql-sarif artifacts/codeql-results/java.sarif \
    --semgrep-json artifacts/semgrep-results.json \
    --gitleaks-sarif artifacts/gitleaks-results.sarif \
    --depcheck-json artifacts/dependency-check/dependency-check-report.json \
    --output data/findings.csv
```

**Inputs:** SARIF/JSON files from CI artifacts
**Output:** Normalized rows appended to findings.csv

## Tool 3: collect_metrics.py

Pulls CI run data (duration, pass/fail, attempts) from the GitHub Actions API.

```bash
export GITHUB_TOKEN=ghp_your_token_here
python tools/collect_metrics.py \
    --repo your-username/security-study \
    --pr 3 \
    --task T1 \
    --condition high-ai \
    --ai-share 0.82 \
    --output data/pr-metrics.csv
```

**Requires:** `GITHUB_TOKEN` with repo read + actions read permissions
**Inputs:** GitHub API data
**Output:** Row appended to pr-metrics.csv + detailed JSON

## Tool 4: analyze.py

Statistical analysis and visualization for RQ1, RQ2, RQ3.

```bash
# Run with your real data:
python tools/analyze.py \
    --metrics data/pr-metrics.csv \
    --findings data/findings.csv \
    --outdir results/

# Test with synthetic demo data (18 PRs):
python tools/analyze.py --metrics x --findings x --demo --outdir results/
```

**Outputs:**
- `rq1_baseline_weaknesses.png` — Box plot: weaknesses by condition (baseline)
- `rq1_scatter.png` — Scatter: AI Share vs weakness count (continuous)
- `rq2_gate_effectiveness.png` — Paired comparison: baseline vs security-gated
- `rq2_duration_overhead.png` — Box plot: CI duration overhead
- `rq3_cwe_heatmap.png` — Heatmap: CWE types by condition
- `rq3_severity_by_condition.png` — Stacked bar: severity by condition
- `analysis_summary.txt` — Text summary of all results

## Typical Workflow Per PR

```bash
# 1. After PR is ready (on your feature branch):
git diff main...HEAD > pr-3-final.diff

# 2. Open PR, wait for both pipelines to complete

# 3. Download artifacts from GitHub Actions UI

# 4. Switch to results branch:
git checkout results

# 5. Move AI patches and final diff:
cp ~/scratch/pr-3-*.{diff,txt} data/ai-patches/

# 6. Compute AI Share:
python tools/compute_ai_share.py --pr 3 --patches-dir data/ai-patches --verbose

# 7. Parse findings:
python tools/parse_findings.py --pr 3 --task T1 --condition high-ai \
    --ai-share 0.82 --pipeline security-gated \
    --codeql-sarif artifacts/codeql-results/java.sarif \
    --semgrep-json artifacts/semgrep-results.json

# 8. Collect pipeline metrics:
python tools/collect_metrics.py --repo you/security-study --pr 3 \
    --task T1 --condition high-ai --ai-share 0.82

# 9. Commit and return to main:
git add -A && git commit -m "PR #3 results"
git checkout main

# 10. After all 18 PRs, run analysis:
git checkout results
python tools/analyze.py --metrics data/pr-metrics.csv --findings data/findings.csv
```
