#!/usr/bin/env python3
"""
Pipeline Metrics Collector

Pulls CI run data from GitHub Actions API for each PR and outputs
structured metrics (duration, pass/fail, run attempts).

Usage:
    python collect_metrics.py \
        --repo owner/repo \
        --pr 3 \
        --task T1 \
        --condition high-ai \
        --ai-share 0.82 \
        --output data/pr-metrics.csv

Requires:
    GITHUB_TOKEN environment variable (with repo read access)

    pip install requests
"""

import argparse
import csv
import json
import os
import sys
from datetime import datetime

try:
    import requests
except ImportError:
    print("ERROR: Install requests first: pip install requests")
    sys.exit(1)


GITHUB_API = "https://api.github.com"


def get_headers():
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        print("ERROR: GITHUB_TOKEN environment variable required")
        print("Create one at: https://github.com/settings/tokens")
        print("Needs: repo (read) and actions (read) permissions")
        sys.exit(1)
    return {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }


def get_workflow_runs(repo: str, pr_number: int, workflow_name: str) -> list[dict]:
    """Get all workflow runs for a specific PR and workflow."""
    url = f"{GITHUB_API}/repos/{repo}/actions/runs"
    params = {
        "event": "pull_request",
        "per_page": 100,
    }
    resp = requests.get(url, headers=get_headers(), params=params)
    resp.raise_for_status()

    runs = []
    for run in resp.json().get("workflow_runs", []):
        # Match by workflow name and PR number
        if run.get("name") != workflow_name:
            continue
        pr_numbers = [pr["number"] for pr in run.get("pull_requests", [])]
        if pr_number in pr_numbers:
            runs.append(run)

    return runs


def compute_duration(run: dict) -> float:
    """Compute run duration in seconds from created_at to updated_at."""
    created = datetime.fromisoformat(run["created_at"].replace("Z", "+00:00"))
    updated = datetime.fromisoformat(run["updated_at"].replace("Z", "+00:00"))
    return (updated - created).total_seconds()


def get_job_details(repo: str, run_id: int) -> list[dict]:
    """Get individual job details for a workflow run."""
    url = f"{GITHUB_API}/repos/{repo}/actions/runs/{run_id}/jobs"
    resp = requests.get(url, headers=get_headers())
    resp.raise_for_status()

    jobs = []
    for job in resp.json().get("jobs", []):
        started = job.get("started_at")
        completed = job.get("completed_at")
        duration = 0
        if started and completed:
            s = datetime.fromisoformat(started.replace("Z", "+00:00"))
            c = datetime.fromisoformat(completed.replace("Z", "+00:00"))
            duration = (c - s).total_seconds()

        jobs.append({
            "name": job["name"],
            "status": job["status"],
            "conclusion": job.get("conclusion", ""),
            "duration_s": duration,
        })

    return jobs


def collect_pipeline_metrics(repo: str, pr_number: int) -> dict:
    """Collect metrics for both pipelines for a given PR."""
    metrics = {}

    for pipeline_name, workflow_name in [
        ("baseline", "Baseline Pipeline"),
        ("security-gated", "Security-Gated Pipeline"),
    ]:
        print(f"\n  Fetching {pipeline_name} runs for PR #{pr_number}...")
        runs = get_workflow_runs(repo, pr_number, workflow_name)

        if not runs:
            print(f"    No runs found for {workflow_name}")
            metrics[pipeline_name] = {
                "pass": "",
                "duration_s": "",
                "run_id": "",
                "run_attempts": 0,
                "jobs": [],
            }
            continue

        # Take the most recent run
        latest_run = runs[0]
        duration = compute_duration(latest_run)
        conclusion = latest_run.get("conclusion", "unknown")
        run_id = latest_run["id"]

        # Get job-level details
        jobs = get_job_details(repo, run_id)

        metrics[pipeline_name] = {
            "pass": conclusion == "success",
            "duration_s": round(duration, 1),
            "run_id": run_id,
            "run_attempts": len(runs),
            "jobs": jobs,
        }

        print(f"    Status:   {conclusion}")
        print(f"    Duration: {duration:.0f}s")
        print(f"    Run ID:   {run_id}")
        print(f"    Attempts: {len(runs)}")

        if jobs:
            print(f"    Jobs:")
            for job in jobs:
                status_icon = "✅" if job["conclusion"] == "success" else "❌"
                print(f"      {status_icon} {job['name']}: {job['duration_s']:.0f}s")

    return metrics


def count_findings_from_jobs(jobs: list[dict]) -> dict:
    """Estimate finding counts from job conclusions."""
    counts = {
        "codeql_findings": "",
        "semgrep_findings": "",
        "gitleaks_findings": "",
        "depcheck_findings": "",
    }
    tool_job_map = {
        "CodeQL SAST": "codeql_findings",
        "Semgrep SAST": "semgrep_findings",
        "Gitleaks Secret Scan": "gitleaks_findings",
        "OWASP Dependency-Check": "depcheck_findings",
    }
    for job in jobs:
        if job["name"] in tool_job_map:
            key = tool_job_map[job["name"]]
            # If job failed, there were findings (exact count needs parse_findings.py)
            if job["conclusion"] == "failure":
                counts[key] = ">0 (use parse_findings.py for exact count)"
            elif job["conclusion"] == "success":
                counts[key] = "0"
    return counts


def main():
    parser = argparse.ArgumentParser(description="Collect CI pipeline metrics from GitHub")
    parser.add_argument("--repo", required=True, help="GitHub repo (owner/name)")
    parser.add_argument("--pr", type=int, required=True, help="PR number")
    parser.add_argument("--task", required=True, help="Task ID (T1–T6)")
    parser.add_argument("--condition", required=True, choices=["human-only", "low-ai", "high-ai"])
    parser.add_argument("--ai-share", type=float, required=True)
    parser.add_argument("--output", default="data/pr-metrics.csv")
    args = parser.parse_args()

    print(f"Collecting pipeline metrics for PR #{args.pr}...")
    metrics = collect_pipeline_metrics(args.repo, args.pr)

    baseline = metrics.get("baseline", {})
    secgated = metrics.get("security-gated", {})
    finding_counts = count_findings_from_jobs(secgated.get("jobs", []))

    # Get line stats from git (you can also pass these as args)
    # For now, leave blank — fill manually or from git diff --stat
    row = {
        "pr_number": args.pr,
        "task_id": args.task,
        "task_name": "",
        "condition": args.condition,
        "ai_share_measured": args.ai_share,
        "lines_added": "",
        "lines_deleted": "",
        "files_changed": "",
        "baseline_pass": baseline.get("pass", ""),
        "baseline_duration_s": baseline.get("duration_s", ""),
        "baseline_run_id": baseline.get("run_id", ""),
        "secgate_pass": secgated.get("pass", ""),
        "secgate_duration_s": secgated.get("duration_s", ""),
        "secgate_run_id": secgated.get("run_id", ""),
        **finding_counts,
        "manual_review_done": "",
        "manual_findings": "",
        "unique_weaknesses": "",
        "notes": "",
    }

    # Write to CSV
    fieldnames = list(row.keys())
    file_exists = os.path.exists(args.output)
    with open(args.output, "a", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        if not file_exists:
            writer.writeheader()
        writer.writerow(row)

    print(f"\n  Metrics written to {args.output}")

    # Also dump full JSON for reference
    json_path = args.output.replace(".csv", f"-pr{args.pr}-detail.json")
    with open(json_path, "w") as f:
        json.dump(metrics, f, indent=2, default=str)
    print(f"  Full details: {json_path}")


if __name__ == "__main__":
    main()
