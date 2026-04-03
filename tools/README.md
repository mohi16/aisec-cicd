
# Thesis Study Pipeline Scripts

This folder contains a refactored analysis pipeline for your master's thesis case study.

## What changed

The old scripts mixed three different things:

1. raw scanner alerts
2. manually confirmed vulnerabilities
3. pipeline outcomes

These scripts separate them so the analysis matches your study design.

## Files

- `build_manifest.py`  
  Scans your `results/` directory and creates `runs.csv`.

- `batch_compute_ai_share.py`  
  Computes AI share for each run from git refs or diff files.

- `batch_collect_metrics.py`  
  Pulls PR and workflow metrics from the GitHub API.

- `batch_parse_findings.py`  
  Parses CodeQL, Semgrep, SpotBugs/FindSecBugs, Gitleaks, and Dependency-Check artifacts into `findings_raw.csv`.

- `build_vulnerabilities_template.py`  
  Converts raw findings into a weakness-level review template, ready for manual triage.

- `analyze.py`  
  Runs the final statistical analysis using confirmed vulnerabilities and PR-level metrics.

- `common.py`  
  Shared helpers.

## Recommended workflow

### 1. Build the manifest

```powershell
python .\build_manifest.py `
  --results-root "D:\FHTW\MSC\Masterarbeit\POC\thesis-project\results" `
  --output ".\data\runs.csv"
```

- `--results-root` points to the folder that contains `task-*` run directories.
- `--output` is the manifest CSV to create.

Then open `data/runs.csv` and review:

- `include_in_analysis`
- `replacement_of`
- `pr_number`
- `scaffold_ref`
- `ai_snapshot_ref`
- `final_ref`

Only the latest duplicate version per task/condition is included by default, so your original `task-1-human-only` should be excluded and `task-1-human-only-v2` should be included.

### 2. Compute AI share

Preferred mode is git-based because it matches your study design best.

```powershell
python .\batch_compute_ai_share.py `
  --manifest ".\data\runs.csv" `
  --repo-root "D:\FHTW\MSC\Masterarbeit\POC\thesis-project" `
  --output ".\data\ai_share.csv"
```

- `--manifest` is the run list.
- `--repo-root` points to your local git repo.
- `--output` is the AI-share CSV.

This uses:

- `scaffold_ref`
- `ai_snapshot_ref`
- `final_ref`

If those refs are missing, the script falls back to diff-file mode when `ai_snapshot_diff_path` and `final_diff_path` exist.

### 3. Collect GitHub PR and CI metrics

```powershell
python .\batch_collect_metrics.py `
  --manifest ".\data\runs.csv" `
  --repo "OWNER/REPO" `
  --output ".\data\pr_metrics.csv"
```

- `--manifest` is the run list.
- `--repo` is your GitHub repository in `owner/name` form.
- `--output` is the metrics CSV.

This requires `GITHUB_TOKEN` in your environment.

### 4. Parse security findings from artifacts

```powershell
python .\batch_parse_findings.py `
  --manifest ".\data\runs.csv" `
  --output ".\data\findings_raw.csv"
```

- `--manifest` is the run list.
- `--output` is the normalized raw findings CSV.

This script reads artifact paths from the manifest and parses:

- CodeQL SARIF
- Semgrep JSON or SARIF
- SpotBugs / FindSecBugs XML
- Gitleaks SARIF or SARF
- Dependency-Check JSON

### 5. Build the manual triage template

```powershell
python .\build_vulnerabilities_template.py `
  --findings ".\data\findings_raw.csv" `
  --output ".\data\vulnerabilities.csv"
```

- `--findings` is the raw findings CSV.
- `--output` is the weakness-level manual review template.

Then open `data/vulnerabilities.csv` and review every row.

At minimum, fill:

- `confirmed_status`  
  Use `TP`, `FP`, or `UNSURE`.

- `reviewer_notes`

- `manual_severity` and `manual_cwe_id` when tool metadata is wrong.

For manual-review-only issues that no tool found, add a new row with:

- `manual_only=true`
- `detected_by_gate=false`
- `escaped_gate=true`
- `source=manual`
- `confirmed_status=TP`

### 6. Run the final analysis

```powershell
python .\analyze.py `
  --manifest ".\data\runs.csv" `
  --metrics ".\data\pr_metrics.csv" `
  --ai-share ".\data\ai_share.csv" `
  --vulnerabilities ".\data\vulnerabilities.csv" `
  --outdir ".\analysis"
```

- `--manifest` is the run metadata.
- `--metrics` is the PR and CI metrics file.
- `--ai-share` is the AI-share file.
- `--vulnerabilities` is the manually confirmed weakness-level dataset.
- `--outdir` is where plots and summaries go.

## Why this matches the thesis better

This version makes the study design explicit:

- `runs.csv` = experimental units and inclusion decisions
- `ai_share.csv` = AI contribution measure
- `pr_metrics.csv` = pipeline cost and pass/fail data
- `findings_raw.csv` = raw scanner alerts
- `vulnerabilities.csv` = manually confirmed ground truth
- `analysis/` = final RQ outputs

That lets you answer:

- **RQ1** with confirmed vulnerabilities per PR
- **RQ2** with gate catch/miss coverage and CI overhead
- **RQ3** with confirmed CWE and severity patterns

## Python packages

Install these first:

```powershell
pip install pandas matplotlib numpy scipy requests
```

## Notes

- SpotBugs / FindSecBugs support is included.
- Gitleaks `.sarif` and `.sarf` are both supported.
- The analysis script uses the manually reviewed vulnerability table, not raw findings, for the final RQs.
