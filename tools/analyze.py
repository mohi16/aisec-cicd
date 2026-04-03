
#!/usr/bin/env python3
from __future__ import annotations

import argparse
import math
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

from common import CONDITION_ORDER, parse_bool, parse_float

FIG_SIZE = (8, 5)
DPI = 180
PALETTE = {"human-only": "#4C72B0", "low-ai": "#55A868", "high-ai": "#C44E52"}
SEVERITY_ORDER = ["critical", "high", "medium", "low", "info"]


def read_csv(path: str | None) -> pd.DataFrame:
    if not path:
        return pd.DataFrame()
    csv_path = Path(path)
    if not csv_path.exists():
        return pd.DataFrame()
    return pd.read_csv(csv_path)


def bool_series(series: pd.Series, default: bool = False) -> pd.Series:
    return series.fillna(default).astype(str).str.strip().str.lower().isin(["1", "true", "yes", "y"])


def normalize_confirmed_status(series: pd.Series) -> pd.Series:
    return series.fillna("").astype(str).str.strip().str.upper()


def prepare_core_runs(manifest: pd.DataFrame, metrics: pd.DataFrame, ai_share: pd.DataFrame) -> pd.DataFrame:
    if manifest.empty:
        raise SystemExit("Manifest is required.")
    manifest = manifest.copy()
    manifest["include_in_analysis"] = bool_series(manifest["include_in_analysis"], default=False)
    core = manifest[manifest["include_in_analysis"]].copy()

    if not metrics.empty:
        core = core.merge(
            metrics,
            on="run_id",
            how="left",
            suffixes=("", "_metrics"),
        )

    if not ai_share.empty:
        ai_share = ai_share[["run_id", "ai_share", "status"]].rename(
            columns={"ai_share": "ai_share_computed", "status": "ai_share_status"}
        )
        core = core.merge(ai_share, on="run_id", how="left")

    if "ai_share_measured" in core.columns:
        core["ai_share"] = pd.to_numeric(core["ai_share_measured"], errors="coerce")
    else:
        core["ai_share"] = np.nan

    if "ai_share_computed" in core.columns:
        mask = core["ai_share"].isna()
        core.loc[mask, "ai_share"] = pd.to_numeric(core.loc[mask, "ai_share_computed"], errors="coerce")

    numeric_columns = [
        "baseline_duration_s",
        "security_duration_s",
        "baseline_execution_s",
        "security_execution_s",
        "additions",
        "deletions",
        "changed_files",
    ]
    for column in numeric_columns:
        if column in core.columns:
            core[column] = pd.to_numeric(core[column], errors="coerce")

    for column in ["baseline_pass", "security_pass"]:
        if column in core.columns:
            core[column] = bool_series(core[column], default=False)
        else:
            core[column] = False

    core["task_number"] = pd.to_numeric(core["task_number"], errors="coerce")
    return core


def prepare_vulnerabilities(vulnerabilities: pd.DataFrame) -> pd.DataFrame:
    if vulnerabilities.empty:
        return vulnerabilities

    vulns = vulnerabilities.copy()
    vulns["include_in_analysis"] = bool_series(vulns["include_in_analysis"], default=False)
    vulns = vulns[vulns["include_in_analysis"]].copy()
    vulns["confirmed_status"] = normalize_confirmed_status(vulns["confirmed_status"])
    vulns = vulns[vulns["confirmed_status"] == "TP"].copy()

    if vulns.empty:
        return vulns

    vulns["manual_only"] = bool_series(vulns["manual_only"], default=False)
    vulns["detected_by_gate"] = bool_series(vulns["detected_by_gate"], default=False)
    vulns["escaped_gate"] = bool_series(vulns["escaped_gate"], default=False)
    vulns["final_severity"] = vulns["manual_severity"].fillna("").replace("", pd.NA).fillna(vulns["severity"])
    vulns["final_cwe_id"] = vulns["manual_cwe_id"].fillna("").replace("", pd.NA).fillna(vulns["cwe_id"])
    vulns["gate_caught"] = vulns["detected_by_gate"] & (~vulns["manual_only"])
    vulns["gate_missed"] = (~vulns["gate_caught"]) | vulns["escaped_gate"] | vulns["manual_only"]
    return vulns


def summarize_per_run(core: pd.DataFrame, vulns: pd.DataFrame) -> pd.DataFrame:
    summary = core.copy()
    if vulns.empty:
        summary["confirmed_vulnerabilities"] = 0
        summary["confirmed_high_critical"] = 0
        summary["gate_caught_count"] = 0
        summary["gate_missed_count"] = 0
        summary["gate_detection_rate"] = np.nan
    else:
        per_run = (
            vulns.groupby("run_id")
            .agg(
                confirmed_vulnerabilities=("vulnerability_id", "count"),
                confirmed_high_critical=("final_severity", lambda s: s.isin(["high", "critical"]).sum()),
                gate_caught_count=("gate_caught", "sum"),
                gate_missed_count=("gate_missed", "sum"),
            )
            .reset_index()
        )
        summary = summary.merge(per_run, on="run_id", how="left")
        for column in [
            "confirmed_vulnerabilities",
            "confirmed_high_critical",
            "gate_caught_count",
            "gate_missed_count",
        ]:
            summary[column] = summary[column].fillna(0).astype(int)
        summary["gate_detection_rate"] = np.where(
            summary["confirmed_vulnerabilities"] > 0,
            summary["gate_caught_count"] / summary["confirmed_vulnerabilities"],
            np.nan,
        )

    summary["duration_overhead_s"] = summary["security_duration_s"] - summary["baseline_duration_s"]
    summary["baseline_would_merge"] = summary["baseline_pass"]
    summary["gate_blocked_pr"] = ~summary["security_pass"]
    summary["vulnerable_pr"] = summary["confirmed_vulnerabilities"] > 0
    summary["vulnerable_pr_blocked_by_gate"] = summary["vulnerable_pr"] & summary["gate_blocked_pr"]
    summary["vulnerable_pr_passed_gate"] = summary["vulnerable_pr"] & (~summary["gate_blocked_pr"])
    return summary


def describe_by_condition(df: pd.DataFrame, column: str) -> pd.DataFrame:
    return (
        df.groupby("condition")[column]
        .agg(["count", "median", "mean", "min", "max"])
        .reindex(CONDITION_ORDER)
    )


def analyze_rq1(summary: pd.DataFrame, outdir: Path) -> dict[str, float]:
    print("\n" + "=" * 70)
    print("RQ1: confirmed vulnerability burden as AI share increases")
    print("=" * 70)

    subset = summary.dropna(subset=["ai_share"]).copy()
    if len(subset) >= 3 and subset["confirmed_vulnerabilities"].nunique() > 1 and subset["ai_share"].nunique() > 1:
        rho, p_value = stats.spearmanr(subset["ai_share"], subset["confirmed_vulnerabilities"])
    else:
        rho, p_value = np.nan, np.nan

    print(f"\nSpearman correlation (AI share vs confirmed vulnerabilities):")
    print(f"  rho = {rho:.3f}" if not np.isnan(rho) else "  rho = NA")
    print(f"  p   = {p_value:.4f}" if not np.isnan(p_value) else "  p   = NA")

    groups = [
        summary.loc[summary["condition"] == condition, "confirmed_vulnerabilities"].values
        for condition in CONDITION_ORDER
        if (summary["condition"] == condition).any()
    ]
    flat_values = np.concatenate([group for group in groups if len(group) > 0]) if groups else np.array([])
    if len(groups) >= 2 and all(len(group) > 0 for group in groups) and len(np.unique(flat_values)) > 1:
        h_stat, kw_p = stats.kruskal(*groups)
        print(f"\nKruskal-Wallis by condition:")
        print(f"  H = {h_stat:.3f}")
        print(f"  p = {kw_p:.4f}")
    else:
        h_stat, kw_p = np.nan, np.nan

    print("\nDescriptive statistics by condition:")
    print(describe_by_condition(summary, "confirmed_vulnerabilities").to_string())

    fig, ax = plt.subplots(figsize=FIG_SIZE)
    positions = np.arange(len(CONDITION_ORDER))
    data = [summary.loc[summary["condition"] == condition, "confirmed_vulnerabilities"].values for condition in CONDITION_ORDER]
    bp = ax.boxplot(data, positions=positions, widths=0.5, patch_artist=True, showfliers=False)
    for patch, condition in zip(bp["boxes"], CONDITION_ORDER):
        patch.set_facecolor(PALETTE[condition])
        patch.set_alpha(0.75)
    for index, condition in enumerate(CONDITION_ORDER):
        values = summary.loc[summary["condition"] == condition, "confirmed_vulnerabilities"].values
        if len(values) == 0:
            continue
        jitter = np.random.default_rng(42 + index).uniform(-0.08, 0.08, len(values))
        ax.scatter(np.full(len(values), index) + jitter, values, edgecolors="black", zorder=3)
        ax.hlines(values.mean(), index - 0.2, index + 0.2, colors="red", linestyles="dashed", linewidth=1.3)
    ax.set_xticks(positions)
    ax.set_xticklabels(CONDITION_ORDER)
    ax.set_xlabel("Condition")
    ax.set_ylabel("Confirmed vulnerabilities per PR")
    title = "RQ1: Confirmed vulnerabilities by condition"
    if not np.isnan(rho):
        title += f"\nSpearman ρ={rho:.3f}, p={p_value:.4f}"
    ax.set_title(title)
    fig.tight_layout()
    fig.savefig(outdir / "rq1_confirmed_vulnerabilities_by_condition.png", dpi=DPI)
    plt.close(fig)

    fig, ax = plt.subplots(figsize=FIG_SIZE)
    for condition in CONDITION_ORDER:
        group = subset[subset["condition"] == condition]
        if group.empty:
            continue
        ax.scatter(
            group["ai_share"],
            group["confirmed_vulnerabilities"],
            s=80,
            edgecolors="black",
            label=condition,
            color=PALETTE[condition],
        )
    if len(subset) >= 2:
        coeffs = np.polyfit(subset["ai_share"], subset["confirmed_vulnerabilities"], 1)
        x_range = np.linspace(subset["ai_share"].min(), subset["ai_share"].max(), 100)
        ax.plot(x_range, np.polyval(coeffs, x_range), linestyle="--", color="gray", linewidth=1.5)
    ax.set_xlabel("AI share")
    ax.set_ylabel("Confirmed vulnerabilities per PR")
    ax.set_title("RQ1: AI share vs confirmed vulnerability burden")
    ax.legend()
    fig.tight_layout()
    fig.savefig(outdir / "rq1_ai_share_scatter.png", dpi=DPI)
    plt.close(fig)

    return {
        "spearman_rho": float(rho) if not np.isnan(rho) else math.nan,
        "spearman_p": float(p_value) if not np.isnan(p_value) else math.nan,
        "kruskal_h": float(h_stat) if not np.isnan(h_stat) else math.nan,
        "kruskal_p": float(kw_p) if not np.isnan(kw_p) else math.nan,
    }


def analyze_rq2(summary: pd.DataFrame, outdir: Path) -> dict[str, float]:
    print("\n" + "=" * 70)
    print("RQ2: security-gated pipeline coverage and operational cost")
    print("=" * 70)

    print("\nPer-condition gate coverage:")
    for condition in CONDITION_ORDER:
        group = summary[summary["condition"] == condition]
        if group.empty:
            continue
        total = int(group["confirmed_vulnerabilities"].sum())
        caught = int(group["gate_caught_count"].sum())
        missed = int(group["gate_missed_count"].sum())
        rate = (caught / total * 100.0) if total else 0.0
        print(f"  {condition}: caught {caught}/{total} ({rate:.1f}%), missed {missed}")

    print("\nPer-condition PR blocking:")
    for condition in CONDITION_ORDER:
        group = summary[summary["condition"] == condition]
        if group.empty:
            continue
        blocked = int(group["gate_blocked_pr"].sum())
        vulnerable = int(group["vulnerable_pr"].sum())
        vulnerable_blocked = int(group["vulnerable_pr_blocked_by_gate"].sum())
        vulnerable_passed = int(group["vulnerable_pr_passed_gate"].sum())
        print(
            f"  {condition}: blocked PRs {blocked}/{len(group)}, vulnerable PRs blocked {vulnerable_blocked}/{vulnerable}, vulnerable PRs passed gate {vulnerable_passed}"
        )

    print("\nDuration overhead by condition:")
    for condition in CONDITION_ORDER:
        group = summary[summary["condition"] == condition]
        if group.empty:
            continue
        median_overhead = group["duration_overhead_s"].median()
        print(f"  {condition}: median overhead {median_overhead:.1f}s")

    fig, ax = plt.subplots(figsize=FIG_SIZE)
    positions = np.arange(len(CONDITION_ORDER))
    caught = [summary.loc[summary["condition"] == condition, "gate_caught_count"].sum() for condition in CONDITION_ORDER]
    missed = [summary.loc[summary["condition"] == condition, "gate_missed_count"].sum() for condition in CONDITION_ORDER]
    ax.bar(positions, caught, label="Caught by gate", color="#4C72B0")
    ax.bar(positions, missed, bottom=caught, label="Missed by gate", color="#C44E52")
    ax.set_xticks(positions)
    ax.set_xticklabels(CONDITION_ORDER)
    ax.set_xlabel("Condition")
    ax.set_ylabel("Confirmed vulnerabilities")
    ax.set_title("RQ2: Gate coverage of confirmed vulnerabilities")
    ax.legend()
    fig.tight_layout()
    fig.savefig(outdir / "rq2_gate_coverage.png", dpi=DPI)
    plt.close(fig)

    fig, ax = plt.subplots(figsize=FIG_SIZE)
    data = [summary.loc[summary["condition"] == condition, "duration_overhead_s"].dropna().values for condition in CONDITION_ORDER]
    bp = ax.boxplot(data, positions=positions, widths=0.5, patch_artist=True, showfliers=False)
    for patch, condition in zip(bp["boxes"], CONDITION_ORDER):
        patch.set_facecolor(PALETTE[condition])
        patch.set_alpha(0.75)
    for index, condition in enumerate(CONDITION_ORDER):
        values = summary.loc[summary["condition"] == condition, "duration_overhead_s"].dropna().values
        if len(values) == 0:
            continue
        jitter = np.random.default_rng(142 + index).uniform(-0.08, 0.08, len(values))
        ax.scatter(np.full(len(values), index) + jitter, values, edgecolors="black", zorder=3)
    ax.axhline(0, color="gray", linestyle="--", linewidth=1)
    ax.set_xticks(positions)
    ax.set_xticklabels(CONDITION_ORDER)
    ax.set_xlabel("Condition")
    ax.set_ylabel("Security-gated minus baseline duration (s)")
    ax.set_title("RQ2: CI duration overhead by condition")
    fig.tight_layout()
    fig.savefig(outdir / "rq2_duration_overhead.png", dpi=DPI)
    plt.close(fig)

    return {
        "total_confirmed_vulns": int(summary["confirmed_vulnerabilities"].sum()),
        "total_caught": int(summary["gate_caught_count"].sum()),
        "total_missed": int(summary["gate_missed_count"].sum()),
    }


def analyze_rq3(vulns: pd.DataFrame, outdir: Path) -> None:
    print("\n" + "=" * 70)
    print("RQ3: CWE and severity patterns by AI involvement")
    print("=" * 70)

    if vulns.empty:
        print("No confirmed vulnerabilities available for RQ3.")
        return

    cwe_cross = pd.crosstab(vulns["final_cwe_id"], vulns["condition"]).reindex(columns=CONDITION_ORDER, fill_value=0)
    severity_cross = pd.crosstab(vulns["final_severity"], vulns["condition"]).reindex(
        index=SEVERITY_ORDER, columns=CONDITION_ORDER, fill_value=0
    )

    print("\nCWE distribution:")
    print(cwe_cross.to_string())
    print("\nSeverity distribution:")
    print(severity_cross.to_string())

    if not cwe_cross.empty:
        fig, ax = plt.subplots(figsize=(10, max(4, len(cwe_cross) * 0.45)))
        heatmap_data = cwe_cross.values
        im = ax.imshow(heatmap_data, aspect="auto")
        ax.set_xticks(np.arange(len(cwe_cross.columns)))
        ax.set_xticklabels(cwe_cross.columns)
        ax.set_yticks(np.arange(len(cwe_cross.index)))
        ax.set_yticklabels(cwe_cross.index)
        ax.set_xlabel("Condition")
        ax.set_ylabel("CWE")
        ax.set_title("RQ3: Confirmed CWE distribution by condition")
        for i in range(heatmap_data.shape[0]):
            for j in range(heatmap_data.shape[1]):
                ax.text(j, i, int(heatmap_data[i, j]), ha="center", va="center", color="black")
        fig.colorbar(im, ax=ax, fraction=0.03, pad=0.02)
        fig.tight_layout()
        fig.savefig(outdir / "rq3_cwe_heatmap.png", dpi=DPI)
        plt.close(fig)

    if not severity_cross.empty:
        fig, ax = plt.subplots(figsize=FIG_SIZE)
        bottom = np.zeros(len(CONDITION_ORDER))
        colors = {
            "critical": "#8b0000",
            "high": "#d95f02",
            "medium": "#e6ab02",
            "low": "#66a61e",
            "info": "#7570b3",
        }
        for severity in SEVERITY_ORDER:
            values = severity_cross.loc[severity, CONDITION_ORDER].values
            ax.bar(CONDITION_ORDER, values, bottom=bottom, label=severity, color=colors[severity])
            bottom += values
        ax.set_xlabel("Condition")
        ax.set_ylabel("Confirmed vulnerabilities")
        ax.set_title("RQ3: Severity distribution by condition")
        ax.legend(title="Severity", bbox_to_anchor=(1.02, 1), loc="upper left")
        fig.tight_layout()
        fig.savefig(outdir / "rq3_severity_distribution.png", dpi=DPI)
        plt.close(fig)


def write_outputs(summary: pd.DataFrame, outdir: Path, rq1_results: dict[str, float], rq2_results: dict[str, float]) -> None:
    outdir.mkdir(parents=True, exist_ok=True)
    summary.to_csv(outdir / "per_run_summary.csv", index=False)

    with (outdir / "analysis_summary.txt").open("w", encoding="utf-8") as handle:
        handle.write("=" * 70 + "\n")
        handle.write("THESIS ANALYSIS SUMMARY\n")
        handle.write("=" * 70 + "\n\n")
        handle.write(f"Included runs: {len(summary)}\n")
        handle.write("Runs by condition:\n")
        for condition in CONDITION_ORDER:
            group = summary[summary["condition"] == condition]
            handle.write(f"  - {condition}: {len(group)}\n")

        handle.write("\nRQ1\n")
        handle.write(f"  Spearman rho: {rq1_results['spearman_rho']}\n")
        handle.write(f"  Spearman p:   {rq1_results['spearman_p']}\n")
        handle.write(f"  Kruskal H:    {rq1_results['kruskal_h']}\n")
        handle.write(f"  Kruskal p:    {rq1_results['kruskal_p']}\n")

        handle.write("\nConfirmed vulnerabilities by condition:\n")
        desc = describe_by_condition(summary, "confirmed_vulnerabilities")
        handle.write(desc.to_string())
        handle.write("\n")

        handle.write("\nRQ2\n")
        handle.write(f"  Total confirmed vulnerabilities: {rq2_results['total_confirmed_vulns']}\n")
        handle.write(f"  Total caught by gate:            {rq2_results['total_caught']}\n")
        handle.write(f"  Total missed by gate:            {rq2_results['total_missed']}\n")

        handle.write("\nDuration overhead medians by condition:\n")
        for condition in CONDITION_ORDER:
            group = summary[summary["condition"] == condition]
            if group.empty:
                continue
            handle.write(
                f"  - {condition}: {group['duration_overhead_s'].median():.1f}s\n"
            )


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Analyze thesis runs using confirmed vulnerabilities and gate coverage."
    )
    parser.add_argument(
        "--manifest",
        required=True,
        help="Path to runs.csv.",
    )
    parser.add_argument(
        "--metrics",
        help="Path to pr_metrics.csv. Optional but recommended for RQ2 and CI overhead.",
    )
    parser.add_argument(
        "--ai-share",
        help="Path to ai_share.csv. Optional if ai_share_measured is already present elsewhere.",
    )
    parser.add_argument(
        "--vulnerabilities",
        required=True,
        help="Path to manually reviewed vulnerabilities.csv.",
    )
    parser.add_argument(
        "--outdir",
        default="analysis",
        help="Output directory for figures and summary files.",
    )
    args = parser.parse_args()

    manifest = read_csv(args.manifest)
    metrics = read_csv(args.metrics)
    ai_share = read_csv(args.ai_share)
    vulnerabilities = read_csv(args.vulnerabilities)

    outdir = Path(args.outdir)
    outdir.mkdir(parents=True, exist_ok=True)

    core = prepare_core_runs(manifest, metrics, ai_share)
    vulns = prepare_vulnerabilities(vulnerabilities)
    summary = summarize_per_run(core, vulns)

    rq1_results = analyze_rq1(summary, outdir)
    rq2_results = analyze_rq2(summary, outdir)
    analyze_rq3(vulns, outdir)
    write_outputs(summary, outdir, rq1_results, rq2_results)

    print(f"\nAnalysis complete. Outputs written to: {outdir}")


if __name__ == "__main__":
    main()
