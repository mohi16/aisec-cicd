
#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import requests

from common import load_csv_rows, parse_bool, parse_int, write_csv_rows

GITHUB_API = "https://api.github.com"
DEFAULT_WORKFLOWS = {
    "baseline": "Baseline Pipeline",
    "security-gated": "Security-Gated Pipeline",
}


def github_headers() -> dict[str, str]:
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        raise SystemExit(
            "GITHUB_TOKEN is required. Create a fine-grained token with read access to pull requests and Actions."
        )
    return {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }


def parse_github_time(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def get_paginated(url: str, params: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    session = requests.Session()
    headers = github_headers()
    items: list[dict[str, Any]] = []
    next_url = url
    next_params = dict(params or {})
    while next_url:
        response = session.get(next_url, headers=headers, params=next_params, timeout=30)
        response.raise_for_status()
        data = response.json()
        if isinstance(data, dict):
            page_items = data.get("workflow_runs") or data.get("jobs") or data.get("items") or []
        elif isinstance(data, list):
            page_items = data
        else:
            page_items = []
        items.extend(page_items)
        link = response.links.get("next", {})
        next_url = link.get("url")
        next_params = None
    return items


def get_pull_request(repo: str, pr_number: int) -> dict[str, Any]:
    url = f"{GITHUB_API}/repos/{repo}/pulls/{pr_number}"
    response = requests.get(url, headers=github_headers(), timeout=30)
    response.raise_for_status()
    return response.json()


def get_workflow_runs(
        repo: str,
        pr_number: int,
        workflow_name: str,
        pr_head_ref: str | None = None,
        pr_head_sha: str | None = None,
) -> tuple[list[dict[str, Any]], str]:
    """Return matching workflow runs plus the match strategy used.

    Historical GitHub Actions runs do not always keep a populated pull_requests array.
    So we try, in order:
      1) exact PR number match from run.pull_requests
      2) exact head SHA match
      3) exact head branch match
    """
    url = f"{GITHUB_API}/repos/{repo}/actions/runs"
    params = {"event": "pull_request", "per_page": 100}
    runs = get_paginated(url, params=params)

    exact_pr: list[dict[str, Any]] = []
    exact_sha: list[dict[str, Any]] = []
    exact_branch: list[dict[str, Any]] = []

    for run in runs:
        if run.get("name") != workflow_name:
            continue

        pr_numbers = [pr.get("number") for pr in run.get("pull_requests", [])]
        head_branch = (run.get("head_branch") or "").strip()
        head_sha = (run.get("head_sha") or "").strip()

        if pr_number in pr_numbers:
            exact_pr.append(run)
            continue
        if pr_head_sha and head_sha and head_sha == pr_head_sha:
            exact_sha.append(run)
            continue
        if pr_head_ref and head_branch and head_branch == pr_head_ref:
            exact_branch.append(run)

    for bucket, label in (
            (exact_pr, "pull_requests"),
            (exact_sha, "head_sha"),
            (exact_branch, "head_branch"),
    ):
        if bucket:
            bucket.sort(key=lambda run: run.get("created_at", ""), reverse=True)
            return bucket, label

    return [], "none"


def get_jobs(repo: str, run_id: int) -> list[dict[str, Any]]:
    url = f"{GITHUB_API}/repos/{repo}/actions/runs/{run_id}/jobs"
    return get_paginated(url, params={"per_page": 100})


def duration_seconds(start: datetime | None, end: datetime | None) -> float | None:
    if not start or not end:
        return None
    return round((end - start).total_seconds(), 1)


def collect_one_pipeline(
        repo: str,
        pr_number: int,
        workflow_name: str,
        pr_head_ref: str | None = None,
        pr_head_sha: str | None = None,
) -> dict[str, Any]:
    runs, match_strategy = get_workflow_runs(repo, pr_number, workflow_name, pr_head_ref, pr_head_sha)
    if not runs:
        return {
            "found": False,
            "pass": "",
            "conclusion": "",
            "run_id": "",
            "run_attempts": 0,
            "queued_s": "",
            "execution_s": "",
            "duration_s": "",
            "match_strategy": "none",
        }
    latest = runs[0]
    created_at = parse_github_time(latest.get("created_at"))
    started_at = parse_github_time(latest.get("run_started_at"))
    updated_at = parse_github_time(latest.get("updated_at"))
    execution_s = duration_seconds(started_at, updated_at)
    queued_s = duration_seconds(created_at, started_at)
    duration_s = duration_seconds(created_at, updated_at)

    return {
        "found": True,
        "pass": str(latest.get("conclusion") == "success").lower(),
        "conclusion": latest.get("conclusion", ""),
        "run_id": latest.get("id", ""),
        "run_attempts": len(runs),
        "queued_s": queued_s if queued_s is not None else "",
        "execution_s": execution_s if execution_s is not None else "",
        "duration_s": duration_s if duration_s is not None else "",
        "match_strategy": match_strategy,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Batch-collect GitHub PR and workflow metrics for thesis runs."
    )
    parser.add_argument(
        "--manifest",
        required=True,
        help="Path to runs.csv manifest. PR numbers must be filled in for rows you want to query.",
    )
    parser.add_argument(
        "--repo",
        required=True,
        help="GitHub repository in owner/name form.",
    )
    parser.add_argument(
        "--output",
        default="data/pr_metrics.csv",
        help="Path to write the per-run metrics CSV.",
    )
    parser.add_argument(
        "--baseline-workflow",
        default=DEFAULT_WORKFLOWS["baseline"],
        help="Exact GitHub Actions workflow name for the baseline pipeline.",
    )
    parser.add_argument(
        "--security-workflow",
        default=DEFAULT_WORKFLOWS["security-gated"],
        help="Exact GitHub Actions workflow name for the security-gated pipeline.",
    )
    parser.add_argument(
        "--include-excluded",
        action="store_true",
        help="Query excluded runs too. By default, only include_in_analysis=true rows are processed.",
    )
    args = parser.parse_args()

    manifest_rows = load_csv_rows(Path(args.manifest))
    if not manifest_rows:
        raise SystemExit(f"No rows found in manifest: {args.manifest}")

    output_rows: list[dict[str, Any]] = []
    for row in manifest_rows:
        if not args.include_excluded and not parse_bool(row.get("include_in_analysis"), False):
            continue

        pr_number = parse_int(row.get("pr_number"))
        result = {
            "run_id": row.get("run_id", ""),
            "pr_number": pr_number or "",
            "task_id": row.get("task_id", ""),
            "condition": row.get("condition", ""),
            "version": row.get("version", ""),
            "include_in_analysis": str(parse_bool(row.get("include_in_analysis"), False)).lower(),
            "pr_title": "",
            "additions": "",
            "deletions": "",
            "changed_files": "",
            "baseline_found": "",
            "baseline_pass": "",
            "baseline_conclusion": "",
            "baseline_run_id": "",
            "baseline_run_attempts": "",
            "baseline_queued_s": "",
            "baseline_execution_s": "",
            "baseline_duration_s": "",
            "security_found": "",
            "security_pass": "",
            "security_conclusion": "",
            "security_run_id": "",
            "security_run_attempts": "",
            "security_queued_s": "",
            "security_execution_s": "",
            "security_duration_s": "",
            "notes": "",
        }

        if not pr_number:
            result["notes"] = "Missing pr_number in manifest."
            output_rows.append(result)
            print(f"{result['run_id']}: skipped (missing pr_number)")
            continue

        try:
            pr = get_pull_request(args.repo, pr_number)
            result["pr_title"] = pr.get("title", "")
            result["additions"] = pr.get("additions", "")
            result["deletions"] = pr.get("deletions", "")
            result["changed_files"] = pr.get("changed_files", "")
            pr_head_ref = ((pr.get("head") or {}).get("ref") or row.get("run_id") or "").strip()
            pr_head_sha = ((pr.get("head") or {}).get("sha") or "").strip()

            baseline = collect_one_pipeline(args.repo, pr_number, args.baseline_workflow, pr_head_ref, pr_head_sha)
            security = collect_one_pipeline(args.repo, pr_number, args.security_workflow, pr_head_ref, pr_head_sha)

            result.update(
                {
                    "baseline_found": str(baseline["found"]).lower(),
                    "baseline_pass": baseline["pass"],
                    "baseline_conclusion": baseline["conclusion"],
                    "baseline_run_id": baseline["run_id"],
                    "baseline_run_attempts": baseline["run_attempts"],
                    "baseline_queued_s": baseline["queued_s"],
                    "baseline_execution_s": baseline["execution_s"],
                    "baseline_duration_s": baseline["duration_s"],
                    "baseline_match_strategy": baseline["match_strategy"],
                    "security_found": str(security["found"]).lower(),
                    "security_pass": security["pass"],
                    "security_conclusion": security["conclusion"],
                    "security_run_id": security["run_id"],
                    "security_run_attempts": security["run_attempts"],
                    "security_queued_s": security["queued_s"],
                    "security_execution_s": security["execution_s"],
                    "security_duration_s": security["duration_s"],
                    "security_match_strategy": security["match_strategy"],
                }
            )
            print(
                f"{result['run_id']}: baseline={result['baseline_conclusion'] or 'NA'} ({baseline['match_strategy']}), "
                f"security={result['security_conclusion'] or 'NA'} ({security['match_strategy']})"
            )
        except Exception as exc:
            result["notes"] = str(exc)
            print(f"{result['run_id']}: error ({exc})")

        output_rows.append(result)

    fieldnames = [
        "run_id",
        "pr_number",
        "task_id",
        "condition",
        "version",
        "include_in_analysis",
        "pr_title",
        "additions",
        "deletions",
        "changed_files",
        "baseline_found",
        "baseline_pass",
        "baseline_conclusion",
        "baseline_run_id",
        "baseline_run_attempts",
        "baseline_queued_s",
        "baseline_execution_s",
        "baseline_duration_s",
        "baseline_match_strategy",
        "security_found",
        "security_pass",
        "security_conclusion",
        "security_run_id",
        "security_run_attempts",
        "security_queued_s",
        "security_execution_s",
        "security_duration_s",
        "security_match_strategy",
        "notes",
    ]
    write_csv_rows(Path(args.output), fieldnames, output_rows)
    print(f"\nMetrics written to: {args.output}")


if __name__ == "__main__":
    main()
