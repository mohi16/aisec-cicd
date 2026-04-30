#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from common import CONDITION_ORDER

FIG_SIZE = (8, 5)
DPI = 180

PALETTE = {
    "human-only": "#4C72B0",
    "low-ai": "#55A868",
    "high-ai": "#C44E52",
}

SEVERITY_ORDER = ["critical", "high", "medium", "low", "info"]


def read_csv(path: str | None) -> pd.DataFrame:
    if not path:
        return pd.DataFrame()

    csv_path = Path(path)

    if not csv_path.exists():
        return pd.DataFrame()

    return pd.read_csv(csv_path)


def bool_series(series: pd.Series, default: bool = False) -> pd.Series:
    return (
        series.fillna(default)
        .astype(str)
        .str.strip()
        .str.lower()
        .isin(["1", "true", "yes", "y"])
    )


def normalize_confirmed_status(series: pd.Series) -> pd.Series:
    return series.fillna("").astype(str).str.strip().str.upper()


def add_bar_labels(ax, bars, fmt="{:.0f}", offset=0.05) -> None:
    for bar in bars:
        height = bar.get_height()
        ax.text(
            bar.get_x() + bar.get_width() / 2,
            height + offset,
            fmt.format(height),
            ha="center",
            va="bottom",
            fontsize=10,
            )


def prepare_core_runs(
        manifest: pd.DataFrame,
        metrics: pd.DataFrame,
        ai_share: pd.DataFrame,
) -> pd.DataFrame:
    if manifest.empty:
        raise SystemExit("Manifest is required.")

    manifest = manifest.copy()
    manifest["include_in_analysis"] = bool_series(
        manifest["include_in_analysis"],
        default=False,
    )

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
            columns={
                "ai_share": "ai_share_computed",
                "status": "ai_share_status",
            }
        )

        core = core.merge(ai_share, on="run_id", how="left")

    if "ai_share_measured" in core.columns:
        core["ai_share"] = pd.to_numeric(
            core["ai_share_measured"],
            errors="coerce",
        )
    else:
        core["ai_share"] = np.nan

    if "ai_share_computed" in core.columns:
        mask = core["ai_share"].isna()

        core.loc[mask, "ai_share"] = pd.to_numeric(
            core.loc[mask, "ai_share_computed"],
            errors="coerce",
        )

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

    vulns["include_in_analysis"] = bool_series(
        vulns["include_in_analysis"],
        default=False,
    )

    vulns["confirmed_status"] = normalize_confirmed_status(
        vulns["confirmed_status"],
    )

    vulns = vulns[
        vulns["include_in_analysis"]
        & (vulns["confirmed_status"] == "TP")
        ].copy()

    if vulns.empty:
        return vulns

    vulns["manual_only"] = bool_series(vulns["manual_only"], default=False)
    vulns["detected_by_gate"] = bool_series(vulns["detected_by_gate"], default=False)
    vulns["escaped_gate"] = bool_series(vulns["escaped_gate"], default=False)

    vulns["final_severity"] = (
        vulns["manual_severity"]
        .fillna("")
        .replace("", pd.NA)
        .fillna(vulns["severity"])
        .astype(str)
        .str.strip()
        .str.lower()
    )

    vulns["final_cwe_id"] = (
        vulns["manual_cwe_id"]
        .fillna("")
        .replace("", pd.NA)
        .fillna(vulns["cwe_id"])
        .astype(str)
        .str.strip()
    )

    vulns["gate_caught"] = vulns["detected_by_gate"] & (~vulns["manual_only"])

    vulns["gate_missed"] = (
            (~vulns["gate_caught"])
            | vulns["escaped_gate"]
            | vulns["manual_only"]
    )

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
                confirmed_high_critical=(
                    "final_severity",
                    lambda s: s.isin(["high", "critical"]).sum(),
                ),
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

    summary["duration_overhead_s"] = (
            summary["security_duration_s"] - summary["baseline_duration_s"]
    )

    summary["baseline_would_merge"] = summary["baseline_pass"]
    summary["gate_blocked_pr"] = ~summary["security_pass"]
    summary["vulnerable_pr"] = summary["confirmed_vulnerabilities"] > 0

    summary["vulnerable_pr_blocked_by_gate"] = (
            summary["vulnerable_pr"] & summary["gate_blocked_pr"]
    )

    summary["vulnerable_pr_passed_gate"] = (
            summary["vulnerable_pr"] & (~summary["gate_blocked_pr"])
    )

    return summary


def describe_by_condition(df: pd.DataFrame, column: str) -> pd.DataFrame:
    return (
        df.groupby("condition")[column]
        .agg(["count", "median", "mean", "min", "max"])
        .reindex(CONDITION_ORDER)
    )


def analyze_rq1(summary: pd.DataFrame, outdir: Path) -> dict[str, object]:
    print("\n" + "=" * 70)
    print("RQ1: descriptive vulnerability burden by AI involvement")
    print("=" * 70)

    desc = describe_by_condition(summary, "confirmed_vulnerabilities")

    totals = (
        summary.groupby("condition")["confirmed_vulnerabilities"]
        .sum()
        .reindex(CONDITION_ORDER)
    )

    print("\nDescriptive statistics by condition:")
    print(desc.to_string())

    print("\nTotal confirmed vulnerabilities by condition:")
    print(totals.to_string())

    # Figure 2: total confirmed vulnerabilities by condition
    fig, ax = plt.subplots(figsize=FIG_SIZE)

    positions = np.arange(len(CONDITION_ORDER))
    values = [totals.loc[condition] for condition in CONDITION_ORDER]
    colors = [PALETTE[condition] for condition in CONDITION_ORDER]

    bars = ax.bar(positions, values, color=colors, edgecolor="black", alpha=0.85)

    ax.set_xticks(positions)
    ax.set_xticklabels(CONDITION_ORDER)
    ax.set_xlabel("Condition")
    ax.set_ylabel("Total confirmed vulnerabilities")
    ax.set_title("Total confirmed vulnerabilities by condition")

    add_bar_labels(ax, bars, fmt="{:.0f}", offset=0.05)

    ax.set_ylim(0, max(values) + 1)

    fig.tight_layout()
    fig.savefig(
        outdir / "figure_02_total_confirmed_vulnerabilities_by_condition.png",
        dpi=DPI,
        )
    plt.close(fig)

    return {
        "analysis_type": "descriptive_only",
        "totals_by_condition": totals.to_dict(),
        "descriptive_statistics": desc.to_dict(),
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

        print(
            f"  {condition}: caught {caught}/{total} "
            f"({rate:.1f}%), missed {missed}"
        )

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
            f"  {condition}: blocked PRs {blocked}/{len(group)}, "
            f"vulnerable PRs blocked {vulnerable_blocked}/{vulnerable}, "
            f"vulnerable PRs passed gate {vulnerable_passed}"
        )

    print("\nDuration overhead by condition:")

    duration_medians = (
        summary.groupby("condition")["duration_overhead_s"]
        .median()
        .reindex(CONDITION_ORDER)
    )

    for condition in CONDITION_ORDER:
        median_overhead = duration_medians.loc[condition]
        print(f"  {condition}: median overhead {median_overhead:.1f}s")

    total_confirmed = int(summary["confirmed_vulnerabilities"].sum())
    total_caught = int(summary["gate_caught_count"].sum())
    total_missed = int(summary["gate_missed_count"].sum())

    # Figure 3: caught vs missed confirmed vulnerabilities
    fig, ax = plt.subplots(figsize=FIG_SIZE)

    labels = ["Caught by gate", "Missed by gate"]
    values = [total_caught, total_missed]
    colors = ["#4C72B0", "#C44E52"]

    bars = ax.bar(labels, values, color=colors, edgecolor="black", alpha=0.85)

    ax.set_ylabel("Confirmed vulnerabilities")
    ax.set_title("Gate coverage of confirmed vulnerabilities")

    add_bar_labels(ax, bars, fmt="{:.0f}", offset=0.15)

    ax.set_ylim(0, max(values) + 2)

    fig.tight_layout()
    fig.savefig(
        outdir / "figure_03_gate_coverage_caught_vs_missed.png",
        dpi=DPI,
        )
    plt.close(fig)

    # Figure 4: median CI runtime overhead by condition
    fig, ax = plt.subplots(figsize=FIG_SIZE)

    positions = np.arange(len(CONDITION_ORDER))
    values = [duration_medians.loc[condition] for condition in CONDITION_ORDER]
    colors = [PALETTE[condition] for condition in CONDITION_ORDER]

    bars = ax.bar(positions, values, color=colors, edgecolor="black", alpha=0.85)

    ax.set_xticks(positions)
    ax.set_xticklabels(CONDITION_ORDER)
    ax.set_xlabel("Condition")
    ax.set_ylabel("Median overhead in seconds")
    ax.set_title("Median CI runtime overhead by condition")

    add_bar_labels(ax, bars, fmt="{:.1f}", offset=8)

    ax.set_ylim(0, max(values) + 80)

    fig.tight_layout()
    fig.savefig(
        outdir / "figure_04_median_ci_runtime_overhead_by_condition.png",
        dpi=DPI,
        )
    plt.close(fig)

    return {
        "total_confirmed_vulns": total_confirmed,
        "total_caught": total_caught,
        "total_missed": total_missed,
    }


def analyze_rq3(vulns: pd.DataFrame, outdir: Path) -> None:
    print("\n" + "=" * 70)
    print("RQ3: CWE and severity patterns by AI involvement")
    print("=" * 70)

    if vulns.empty:
        print("No confirmed vulnerabilities available for RQ3.")
        return

    cwe_cross = pd.crosstab(
        vulns["final_cwe_id"],
        vulns["condition"],
    ).reindex(columns=CONDITION_ORDER, fill_value=0)

    severity_cross = pd.crosstab(
        vulns["final_severity"],
        vulns["condition"],
    ).reindex(
        index=SEVERITY_ORDER,
        columns=CONDITION_ORDER,
        fill_value=0,
    )

    print("\nCWE distribution:")
    print(cwe_cross.to_string())

    print("\nSeverity distribution:")
    print(severity_cross.to_string())

    # Figure 5: CWE dot matrix
    if not cwe_cross.empty:
        fig, ax = plt.subplots(figsize=(8, max(4, len(cwe_cross) * 0.55)))

        x_positions = np.arange(len(CONDITION_ORDER))
        y_positions = np.arange(len(cwe_cross.index))

        ax.set_xticks(x_positions)
        ax.set_xticklabels(CONDITION_ORDER)

        ax.set_yticks(y_positions)
        ax.set_yticklabels(cwe_cross.index)

        ax.set_xlabel("Condition")
        ax.set_ylabel("CWE")
        ax.set_title("Confirmed CWE categories by condition")

        for y, cwe in enumerate(cwe_cross.index):
            for x, condition in enumerate(CONDITION_ORDER):
                value = int(cwe_cross.loc[cwe, condition])

                if value > 0:
                    ax.scatter(
                        x,
                        y,
                        s=220,
                        color=PALETTE[condition],
                        edgecolor="black",
                        zorder=3,
                    )
                    ax.text(
                        x,
                        y,
                        str(value),
                        ha="center",
                        va="center",
                        color="white",
                        fontsize=9,
                        fontweight="bold",
                        zorder=4,
                    )
                else:
                    ax.scatter(
                        x,
                        y,
                        s=80,
                        facecolors="none",
                        edgecolor="lightgray",
                        linewidth=1.2,
                        zorder=2,
                    )

        ax.set_xlim(-0.5, len(CONDITION_ORDER) - 0.5)
        ax.set_ylim(len(cwe_cross.index) - 0.5, -0.5)
        ax.grid(axis="x", color="lightgray", linestyle="--", linewidth=0.7)
        ax.grid(axis="y", color="lightgray", linestyle="--", linewidth=0.7)

        fig.tight_layout()
        fig.savefig(
            outdir / "figure_05_confirmed_cwe_categories_dot_matrix.png",
            dpi=DPI,
            )
        plt.close(fig)

    # Figure 6: severity distribution
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

        active_severities = [
            severity
            for severity in SEVERITY_ORDER
            if severity_cross.loc[severity].sum() > 0
        ]

        for severity in active_severities:
            values = severity_cross.loc[severity, CONDITION_ORDER].values

            ax.bar(
                CONDITION_ORDER,
                values,
                bottom=bottom,
                label=severity,
                color=colors[severity],
                edgecolor="black",
                alpha=0.9,
            )

            bottom += values

        for index, total in enumerate(bottom):
            ax.text(
                index,
                total + 0.08,
                f"{int(total)}",
                ha="center",
                va="bottom",
                fontsize=10,
                )

        ax.set_xlabel("Condition")
        ax.set_ylabel("Confirmed vulnerabilities")
        ax.set_title("Severity of confirmed vulnerabilities by condition")

        if active_severities:
            ax.legend(
                title="Severity",
                bbox_to_anchor=(1.02, 1),
                loc="upper left",
            )

        ax.set_ylim(0, max(bottom) + 1)

        fig.tight_layout()
        fig.savefig(
            outdir / "figure_06_severity_distribution_by_condition.png",
            dpi=DPI,
            )
        plt.close(fig)


def analyze_overview(summary: pd.DataFrame, outdir: Path) -> None:
    print("\n" + "=" * 70)
    print("OVERVIEW: task x condition outcome matrix")
    print("=" * 70)

    plot_df = summary.copy()

    plot_df["task_label"] = (
        plot_df["task_number"]
        .astype(int)
        .apply(lambda x: f"T{x}")
    )

    task_order = [f"T{i}" for i in range(1, 7)]

    count_matrix = (
        plot_df.pivot(
            index="task_label",
            columns="condition",
            values="confirmed_vulnerabilities",
        )
        .reindex(index=task_order, columns=CONDITION_ORDER)
        .fillna(0)
        .astype(int)
    )

    gate_matrix = (
        plot_df.pivot(
            index="task_label",
            columns="condition",
            values="gate_blocked_pr",
        )
        .reindex(index=task_order, columns=CONDITION_ORDER)
    )

    print("\nConfirmed vulnerability matrix:")
    print(count_matrix.to_string())

    fig, ax = plt.subplots(figsize=(7.2, 5.2))

    heatmap_data = count_matrix.values

    im = ax.imshow(
        heatmap_data,
        aspect="auto",
        cmap="YlOrRd",
        vmin=0,
        vmax=max(1, int(np.nanmax(heatmap_data))),
    )

    ax.set_xticks(np.arange(len(count_matrix.columns)))
    ax.set_xticklabels(count_matrix.columns)

    ax.set_yticks(np.arange(len(count_matrix.index)))
    ax.set_yticklabels(count_matrix.index)

    ax.set_xlabel("Condition")
    ax.set_ylabel("Task")
    ax.set_title("Confirmed vulnerabilities per task and condition")

    for i in range(heatmap_data.shape[0]):
        for j in range(heatmap_data.shape[1]):
            vuln_count = int(heatmap_data[i, j])

            gate_value = gate_matrix.iloc[i, j]
            gate_blocked = False if pd.isna(gate_value) else bool(gate_value)

            gate_label = "B" if gate_blocked else "P"

            ax.text(
                j,
                i,
                f"{vuln_count}\n{gate_label}",
                ha="center",
                va="center",
                color="black",
                fontsize=9,
                fontweight="bold",
            )

    cbar = fig.colorbar(im, ax=ax, fraction=0.035, pad=0.02)
    cbar.set_label("Confirmed vulnerabilities")

    fig.tight_layout()
    fig.savefig(
        outdir / "figure_01_overview_task_condition_matrix.png",
        dpi=DPI,
        )
    plt.close(fig)

    count_matrix_reset = count_matrix.reset_index().rename(
        columns={"task_label": "task"}
    )

    count_matrix_reset.to_csv(
        outdir / "figure_01_overview_task_condition_matrix.csv",
        index=False,
        )


def write_outputs(
        summary: pd.DataFrame,
        outdir: Path,
        rq1_results: dict[str, object],
        rq2_results: dict[str, float],
) -> None:
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
        handle.write("  Analysis type: descriptive only\n")
        handle.write(
            "  No inferential statistical tests were used because the dataset "
            "contains only 18 runs from one controlled case study.\n"
        )

        handle.write("\nConfirmed vulnerabilities by condition:\n")
        desc = describe_by_condition(summary, "confirmed_vulnerabilities")
        handle.write(desc.to_string())
        handle.write("\n")

        handle.write("\nTotal confirmed vulnerabilities by condition:\n")
        totals = (
            summary.groupby("condition")["confirmed_vulnerabilities"]
            .sum()
            .reindex(CONDITION_ORDER)
        )
        handle.write(totals.to_string())
        handle.write("\n")

        handle.write("\nRQ2\n")
        handle.write(
            f"  Total confirmed vulnerabilities: "
            f"{rq2_results['total_confirmed_vulns']}\n"
        )
        handle.write(
            f"  Total caught by gate:            "
            f"{rq2_results['total_caught']}\n"
        )
        handle.write(
            f"  Total missed by gate:            "
            f"{rq2_results['total_missed']}\n"
        )

        handle.write("\nDuration overhead medians by condition:\n")
        for condition in CONDITION_ORDER:
            group = summary[summary["condition"] == condition]

            if group.empty:
                continue

            handle.write(
                f"  - {condition}: "
                f"{group['duration_overhead_s'].median():.1f}s\n"
            )

        handle.write("\nGenerated figure files:\n")
        handle.write("  - figure_01_overview_task_condition_matrix.png\n")
        handle.write("  - figure_02_total_confirmed_vulnerabilities_by_condition.png\n")
        handle.write("  - figure_03_gate_coverage_caught_vs_missed.png\n")
        handle.write("  - figure_04_median_ci_runtime_overhead_by_condition.png\n")
        handle.write("  - figure_05_confirmed_cwe_categories_dot_matrix.png\n")
        handle.write("  - figure_06_severity_distribution_by_condition.png\n")

        handle.write("\nNotes:\n")
        handle.write(
            "  The reported results describe this specific study dataset only. "
            "They are not used to infer general effects beyond the study setting.\n"
        )


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Analyze thesis runs descriptively using confirmed vulnerabilities "
            "and gate coverage."
        )
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

    analyze_overview(summary, outdir)

    rq1_results = analyze_rq1(summary, outdir)
    rq2_results = analyze_rq2(summary, outdir)

    analyze_rq3(vulns, outdir)

    write_outputs(summary, outdir, rq1_results, rq2_results)

    print(f"\nAnalysis complete. Outputs written to: {outdir}")


if __name__ == "__main__":
    main()