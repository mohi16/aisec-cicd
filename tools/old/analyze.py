#!/usr/bin/env python3
"""
Statistical Analysis for Master Thesis

Analyzes PR metrics and findings data to answer:
  RQ1: How does residual CWE risk change as AI Share increases? (Baseline)
  RQ2: How effective is the Security-gated pipeline at reducing risk, and at what cost?
  RQ3: Do CWE patterns and severity shift with AI Share?

Produces publication-ready figures (box plots + swarm overlays per FH Technikum guidelines)
and statistical test results.

Usage:
    python analyze.py --metrics data/pr-metrics.csv --findings data/findings.csv --outdir results/

Requirements:
    pip install pandas matplotlib seaborn scipy
"""

import argparse
import os
import sys
import warnings
warnings.filterwarnings("ignore")

try:
    import pandas as pd
    import matplotlib.pyplot as plt
    import matplotlib
    matplotlib.use("Agg")  # Non-interactive backend
    import seaborn as sns
    from scipy import stats
except ImportError:
    print("Install required packages:")
    print("  pip install pandas matplotlib seaborn scipy")
    sys.exit(1)


# ── Style ───────────────────────────────────────────────────────────────

sns.set_theme(style="whitegrid", font_scale=1.1)
PALETTE = {"human-only": "#4C72B0", "low-ai": "#55A868", "high-ai": "#C44E52"}
CONDITION_ORDER = ["human-only", "low-ai", "high-ai"]
FIG_SIZE = (8, 5)
DPI = 150


# ── Data Loading ────────────────────────────────────────────────────────

def load_data(metrics_path: str, findings_path: str) -> tuple[pd.DataFrame, pd.DataFrame]:
    metrics = pd.read_csv(metrics_path)
    findings = pd.read_csv(findings_path)

    # Ensure types
    metrics["ai_share_measured"] = pd.to_numeric(metrics["ai_share_measured"], errors="coerce")
    metrics["baseline_duration_s"] = pd.to_numeric(metrics["baseline_duration_s"], errors="coerce")
    metrics["secgate_duration_s"] = pd.to_numeric(metrics["secgate_duration_s"], errors="coerce")

    return metrics, findings


def compute_pr_summaries(metrics: pd.DataFrame, findings: pd.DataFrame) -> pd.DataFrame:
    """Compute per-PR summary statistics from findings."""
    # Count unique weaknesses per PR per pipeline
    weakness_counts = (
        findings[findings["triage_status"] == "TP"]
        .drop_duplicates(subset=["pr_number", "pipeline", "unique_weakness_id"])
        .groupby(["pr_number", "pipeline"])
        .agg(
            weakness_count=("unique_weakness_id", "nunique"),
            high_crit_count=("severity", lambda x: ((x == "high") | (x == "critical")).sum()),
        )
        .reset_index()
    )

    # Pivot so we have baseline and secgated columns
    baseline_wc = weakness_counts[weakness_counts["pipeline"] == "baseline"][
        ["pr_number", "weakness_count", "high_crit_count"]
    ].rename(columns={"weakness_count": "baseline_weaknesses", "high_crit_count": "baseline_high_crit"})

    secgated_wc = weakness_counts[weakness_counts["pipeline"] == "security-gated"][
        ["pr_number", "weakness_count", "high_crit_count"]
    ].rename(columns={"weakness_count": "secgated_weaknesses", "high_crit_count": "secgated_high_crit"})

    df = metrics.merge(baseline_wc, on="pr_number", how="left")
    df = df.merge(secgated_wc, on="pr_number", how="left")
    df = df.fillna(0)

    # Weaknesses prevented = baseline - secgated (what the gate caught)
    df["weaknesses_prevented"] = df["baseline_weaknesses"] - df["secgated_weaknesses"]

    # Duration overhead
    df["duration_overhead_s"] = df["secgate_duration_s"] - df["baseline_duration_s"]
    df["duration_overhead_pct"] = (
        df["duration_overhead_s"] / df["baseline_duration_s"] * 100
    ).round(1)

    return df


# ── RQ1: Baseline residual risk vs AI Share ─────────────────────────────

def analyze_rq1(df: pd.DataFrame, outdir: str):
    print("\n" + "=" * 60)
    print("RQ1: Residual CWE risk in Baseline pipeline vs AI Share")
    print("=" * 60)

    # Spearman correlation: AI Share vs baseline weakness count
    corr, p_value = stats.spearmanr(df["ai_share_measured"], df["baseline_weaknesses"])
    print(f"\n  Spearman correlation (AI Share vs Baseline weaknesses):")
    print(f"    rho = {corr:.3f}, p = {p_value:.4f}")
    sig = "significant" if p_value < 0.05 else "not significant"
    print(f"    → {sig} at α=0.05")

    # Kruskal-Wallis test across conditions
    groups = [
        df[df["condition"] == c]["baseline_weaknesses"].values
        for c in CONDITION_ORDER
        if len(df[df["condition"] == c]) > 0
    ]
    if len(groups) >= 2 and all(len(g) > 0 for g in groups):
        h_stat, kw_p = stats.kruskal(*groups)
        print(f"\n  Kruskal-Wallis (Baseline weaknesses across conditions):")
        print(f"    H = {h_stat:.3f}, p = {kw_p:.4f}")

    # Descriptive stats per condition
    print(f"\n  Descriptive statistics (Baseline weaknesses):")
    desc = df.groupby("condition")["baseline_weaknesses"].describe()
    print(desc.to_string())

    # Figure 1: Box plot + swarm — Baseline weaknesses by condition
    fig, ax = plt.subplots(figsize=FIG_SIZE)
    sns.boxplot(
        data=df, x="condition", y="baseline_weaknesses",
        order=CONDITION_ORDER, palette=PALETTE,
        width=0.5, linewidth=1.5, fliersize=0, ax=ax,
    )
    sns.swarmplot(
        data=df, x="condition", y="baseline_weaknesses",
        order=CONDITION_ORDER, color="black", size=6, ax=ax,
    )
    # Add mean lines
    for i, cond in enumerate(CONDITION_ORDER):
        mean_val = df[df["condition"] == cond]["baseline_weaknesses"].mean()
        ax.hlines(mean_val, i - 0.25, i + 0.25, colors="red", linestyles="dashed", linewidth=1.5)

    ax.set_xlabel("Condition")
    ax.set_ylabel("Unique Weaknesses (Baseline Pipeline)")
    ax.set_title(f"RQ1: Residual Risk by AI Involvement\n(Spearman ρ={corr:.3f}, p={p_value:.4f})")
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "rq1_baseline_weaknesses.png"), dpi=DPI)
    plt.close()
    print(f"\n  → Saved: rq1_baseline_weaknesses.png")

    # Figure 2: Scatter — AI Share (continuous) vs weakness count
    fig, ax = plt.subplots(figsize=FIG_SIZE)
    for cond in CONDITION_ORDER:
        subset = df[df["condition"] == cond]
        ax.scatter(
            subset["ai_share_measured"], subset["baseline_weaknesses"],
            color=PALETTE[cond], label=cond, s=80, edgecolors="black", zorder=3,
        )
    # Trend line
    z = np.polyfit(df["ai_share_measured"], df["baseline_weaknesses"], 1)
    p = np.poly1d(z)
    x_range = np.linspace(df["ai_share_measured"].min(), df["ai_share_measured"].max(), 50)
    ax.plot(x_range, p(x_range), "--", color="gray", alpha=0.7, label=f"trend (ρ={corr:.3f})")

    ax.set_xlabel("AI Share")
    ax.set_ylabel("Unique Weaknesses (Baseline)")
    ax.set_title("RQ1: AI Share vs Residual Risk (Continuous)")
    ax.legend()
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "rq1_scatter.png"), dpi=DPI)
    plt.close()
    print(f"  → Saved: rq1_scatter.png")

    return {"spearman_rho": corr, "spearman_p": p_value}


# ── RQ2: Gate effectiveness and cost ────────────────────────────────────

def analyze_rq2(df: pd.DataFrame, outdir: str):
    print("\n" + "=" * 60)
    print("RQ2: Security gate effectiveness and operational cost")
    print("=" * 60)

    # Weaknesses prevented
    print(f"\n  Weaknesses prevented by security gate:")
    for cond in CONDITION_ORDER:
        subset = df[df["condition"] == cond]
        prevented = subset["weaknesses_prevented"].sum()
        baseline_total = subset["baseline_weaknesses"].sum()
        rate = (prevented / baseline_total * 100) if baseline_total > 0 else 0
        print(f"    {cond}: {prevented:.0f}/{baseline_total:.0f} ({rate:.0f}%)")

    # Wilcoxon signed-rank test: baseline vs secgated weaknesses (paired)
    if len(df) >= 5:
        stat, p_value = stats.wilcoxon(
            df["baseline_weaknesses"], df["secgated_weaknesses"],
            alternative="greater", zero_method="zsplit",
        )
        print(f"\n  Wilcoxon signed-rank (baseline > secgated weaknesses):")
        print(f"    W = {stat:.3f}, p = {p_value:.4f}")

    # Duration overhead
    print(f"\n  CI Duration overhead (security-gated vs baseline):")
    for cond in CONDITION_ORDER:
        subset = df[df["condition"] == cond]
        overhead = subset["duration_overhead_s"].median()
        pct = subset["duration_overhead_pct"].median()
        print(f"    {cond}: +{overhead:.0f}s median (+{pct:.0f}%)")

    # Figure 3: Paired comparison — baseline vs gated weaknesses
    fig, ax = plt.subplots(figsize=FIG_SIZE)
    plot_data = df.melt(
        id_vars=["pr_number", "condition"],
        value_vars=["baseline_weaknesses", "secgated_weaknesses"],
        var_name="pipeline", value_name="weaknesses",
    )
    plot_data["pipeline"] = plot_data["pipeline"].map({
        "baseline_weaknesses": "Baseline",
        "secgated_weaknesses": "Security-Gated",
    })
    sns.boxplot(
        data=plot_data, x="condition", y="weaknesses", hue="pipeline",
        order=CONDITION_ORDER, palette=["#AAAAAA", "#4C72B0"],
        width=0.6, linewidth=1.5, fliersize=0, ax=ax,
    )
    sns.swarmplot(
        data=plot_data, x="condition", y="weaknesses", hue="pipeline",
        order=CONDITION_ORDER, dodge=True, color="black", size=5, ax=ax,
        legend=False,
    )
    ax.set_xlabel("Condition")
    ax.set_ylabel("Unique Weaknesses Reaching Main")
    ax.set_title("RQ2: Baseline vs Security-Gated Pipeline Effectiveness")
    ax.legend(title="Pipeline")
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "rq2_gate_effectiveness.png"), dpi=DPI)
    plt.close()
    print(f"\n  → Saved: rq2_gate_effectiveness.png")

    # Figure 4: Duration overhead by condition
    fig, ax = plt.subplots(figsize=FIG_SIZE)
    sns.boxplot(
        data=df, x="condition", y="duration_overhead_s",
        order=CONDITION_ORDER, palette=PALETTE,
        width=0.5, linewidth=1.5, fliersize=0, ax=ax,
    )
    sns.swarmplot(
        data=df, x="condition", y="duration_overhead_s",
        order=CONDITION_ORDER, color="black", size=6, ax=ax,
    )
    ax.axhline(y=0, color="gray", linestyle="--", alpha=0.5)
    ax.set_xlabel("Condition")
    ax.set_ylabel("Duration Overhead (seconds)")
    ax.set_title("RQ2: Security Gate CI Duration Overhead")
    fig.tight_layout()
    fig.savefig(os.path.join(outdir, "rq2_duration_overhead.png"), dpi=DPI)
    plt.close()
    print(f"  → Saved: rq2_duration_overhead.png")


# ── RQ3: CWE patterns and severity by AI Share ─────────────────────────

def analyze_rq3(findings: pd.DataFrame, df: pd.DataFrame, outdir: str):
    print("\n" + "=" * 60)
    print("RQ3: CWE patterns and severity by AI involvement")
    print("=" * 60)

    tp_findings = findings[findings["triage_status"] == "TP"].copy()

    if tp_findings.empty:
        print("  No confirmed (TP) findings to analyze.")
        return

    # CWE distribution by condition
    print(f"\n  CWE distribution by condition:")
    cwe_cross = pd.crosstab(tp_findings["cwe_id"], tp_findings["condition"])
    if not cwe_cross.empty:
        # Reorder columns
        cols = [c for c in CONDITION_ORDER if c in cwe_cross.columns]
        cwe_cross = cwe_cross[cols]
        print(cwe_cross.to_string())

    # Severity distribution by condition
    print(f"\n  Severity distribution by condition:")
    sev_cross = pd.crosstab(tp_findings["severity"], tp_findings["condition"])
    if not sev_cross.empty:
        cols = [c for c in CONDITION_ORDER if c in sev_cross.columns]
        sev_cross = sev_cross[cols]
        sev_order = ["critical", "high", "medium", "low", "info"]
        sev_cross = sev_cross.reindex([s for s in sev_order if s in sev_cross.index])
        print(sev_cross.to_string())

    # Which CWEs did the security gate prevent vs miss?
    prevented = tp_findings[tp_findings["pipeline"] == "security-gated"]
    baseline_only = tp_findings[tp_findings["pipeline"] == "baseline"]
    if not prevented.empty:
        print(f"\n  CWEs caught by security gate:")
        print(prevented["cwe_id"].value_counts().to_string())

    # Figure 5: CWE heatmap by condition
    if not cwe_cross.empty:
        fig, ax = plt.subplots(figsize=(10, max(4, len(cwe_cross) * 0.5)))
        sns.heatmap(
            cwe_cross, annot=True, fmt="d", cmap="YlOrRd",
            linewidths=0.5, ax=ax,
        )
        ax.set_title("RQ3: CWE Distribution by AI Involvement")
        ax.set_ylabel("CWE Category")
        ax.set_xlabel("Condition")
        fig.tight_layout()
        fig.savefig(os.path.join(outdir, "rq3_cwe_heatmap.png"), dpi=DPI)
        plt.close()
        print(f"\n  → Saved: rq3_cwe_heatmap.png")

    # Figure 6: Severity by condition (stacked bar)
    if not sev_cross.empty:
        fig, ax = plt.subplots(figsize=FIG_SIZE)
        sev_colors = {
            "critical": "#d32f2f", "high": "#f57c00",
            "medium": "#fdd835", "low": "#81c784", "info": "#90a4ae",
        }
        sev_cross.T.plot(
            kind="bar", stacked=True, ax=ax,
            color=[sev_colors.get(s, "#999") for s in sev_cross.index],
        )
        ax.set_xlabel("Condition")
        ax.set_ylabel("Finding Count (TP)")
        ax.set_title("RQ3: Severity Distribution by AI Involvement")
        ax.legend(title="Severity", bbox_to_anchor=(1.05, 1))
        ax.set_xticklabels(ax.get_xticklabels(), rotation=0)
        fig.tight_layout()
        fig.savefig(os.path.join(outdir, "rq3_severity_by_condition.png"), dpi=DPI)
        plt.close()
        print(f"  → Saved: rq3_severity_by_condition.png")

    # Tool coverage: which tools caught what?
    print(f"\n  Tool coverage (TP findings):")
    tool_cwe = pd.crosstab(tp_findings["tool"], tp_findings["cwe_id"])
    if not tool_cwe.empty:
        print(tool_cwe.to_string())


# ── Summary Report ──────────────────────────────────────────────────────

def write_summary(df: pd.DataFrame, findings: pd.DataFrame, rq1_results: dict, outdir: str):
    """Write a text summary suitable for including in the thesis."""
    summary_path = os.path.join(outdir, "analysis_summary.txt")
    with open(summary_path, "w") as f:
        f.write("=" * 70 + "\n")
        f.write("ANALYSIS SUMMARY\n")
        f.write("=" * 70 + "\n\n")

        f.write(f"Total PRs analyzed: {len(df)}\n")
        for cond in CONDITION_ORDER:
            n = len(df[df["condition"] == cond])
            mean_ai = df[df["condition"] == cond]["ai_share_measured"].mean()
            f.write(f"  {cond}: n={n}, mean AI Share={mean_ai:.2f}\n")

        f.write(f"\nTotal findings (all tools): {len(findings)}\n")
        tp = len(findings[findings["triage_status"] == "TP"])
        fp = len(findings[findings["triage_status"] == "FP"])
        f.write(f"  True Positives: {tp}\n")
        f.write(f"  False Positives: {fp}\n")
        f.write(f"  Precision: {tp/(tp+fp)*100:.0f}%\n" if (tp + fp) > 0 else "")

        unique_w = findings.drop_duplicates("unique_weakness_id")
        f.write(f"  Unique weaknesses: {len(unique_w)}\n")

        f.write(f"\nRQ1: Spearman ρ={rq1_results['spearman_rho']:.3f}, "
                f"p={rq1_results['spearman_p']:.4f}\n")

        f.write(f"\nMean weaknesses by condition (Baseline):\n")
        for cond in CONDITION_ORDER:
            mean_w = df[df["condition"] == cond]["baseline_weaknesses"].mean()
            f.write(f"  {cond}: {mean_w:.1f}\n")

        f.write(f"\nMean weaknesses by condition (Security-Gated):\n")
        for cond in CONDITION_ORDER:
            mean_w = df[df["condition"] == cond]["secgated_weaknesses"].mean()
            f.write(f"  {cond}: {mean_w:.1f}\n")

    print(f"\n  → Summary written to: {summary_path}")


# ── Main ────────────────────────────────────────────────────────────────

def main():
    import numpy as np
    # Make numpy available for polyfit in rq1
    globals()["np"] = np

    parser = argparse.ArgumentParser(description="Thesis statistical analysis")
    parser.add_argument("--metrics", required=True, help="Path to pr-metrics.csv")
    parser.add_argument("--findings", required=True, help="Path to findings.csv")
    parser.add_argument("--outdir", default="results/", help="Output directory for figures")
    parser.add_argument("--demo", action="store_true", help="Run with synthetic demo data")
    args = parser.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    if args.demo:
        print("Generating synthetic demo data...")
        df, findings = generate_demo_data()
    else:
        df, findings = load_data(args.metrics, args.findings)

    # Compute per-PR summaries
    df = compute_pr_summaries(df, findings)

    print(f"\nLoaded {len(df)} PRs, {len(findings)} findings")

    # Run analyses
    rq1_results = analyze_rq1(df, args.outdir)
    analyze_rq2(df, args.outdir)
    analyze_rq3(findings, df, args.outdir)
    write_summary(df, findings, rq1_results, args.outdir)

    print(f"\n{'='*60}")
    print(f"Analysis complete. Figures saved to: {args.outdir}")
    print(f"{'='*60}")


# ── Demo Data Generator ─────────────────────────────────────────────────

def generate_demo_data() -> tuple[pd.DataFrame, pd.DataFrame]:
    """Generate realistic synthetic data for 18 PRs to test the analysis pipeline."""
    import numpy as np
    np.random.seed(42)

    conditions = ["human-only"] * 6 + ["low-ai"] * 6 + ["high-ai"] * 6
    tasks = ["T1", "T2", "T3", "T4", "T5", "T6"] * 3
    ai_shares = (
        list(np.random.uniform(0.0, 0.02, 6))    # human-only
        + list(np.random.uniform(0.18, 0.42, 6))  # low-ai
        + list(np.random.uniform(0.62, 0.91, 6))  # high-ai
    )

    metrics_rows = []
    findings_rows = []
    finding_counter = 1
    weakness_counter = 1

    cwes_by_likelihood = {
        "human-only": ["CWE-200", "CWE-532", "CWE-20"],
        "low-ai": ["CWE-89", "CWE-200", "CWE-798", "CWE-20"],
        "high-ai": ["CWE-89", "CWE-798", "CWE-327", "CWE-22", "CWE-862", "CWE-209"],
    }

    for i in range(18):
        pr = i + 1
        cond = conditions[i]
        task = tasks[i]
        ai_share = round(ai_shares[i], 4)

        # More weaknesses with higher AI share (with noise)
        base_weakness_count = int(np.random.poisson(
            lam=1.0 + ai_share * 3.5
        ))
        base_weakness_count = max(0, min(base_weakness_count, 8))

        # Security gate catches some (60-80%)
        gate_catch_rate = np.random.uniform(0.55, 0.85)
        gated_weakness_count = max(0, base_weakness_count - int(base_weakness_count * gate_catch_rate))

        baseline_duration = np.random.uniform(45, 80)
        secgate_duration = baseline_duration + np.random.uniform(90, 200)

        metrics_rows.append({
            "pr_number": pr,
            "task_id": task,
            "task_name": f"Task {task}",
            "condition": cond,
            "ai_share_measured": ai_share,
            "lines_added": np.random.randint(80, 350),
            "lines_deleted": np.random.randint(5, 50),
            "files_changed": np.random.randint(3, 8),
            "baseline_pass": True,
            "baseline_duration_s": round(baseline_duration, 1),
            "baseline_run_id": 10000 + pr,
            "secgate_pass": gated_weakness_count == 0,
            "secgate_duration_s": round(secgate_duration, 1),
            "secgate_run_id": 20000 + pr,
        })

        # Generate findings
        available_cwes = cwes_by_likelihood[cond]
        severities = {"CWE-89": "high", "CWE-798": "high", "CWE-327": "medium",
                       "CWE-22": "high", "CWE-862": "high", "CWE-200": "medium",
                       "CWE-532": "low", "CWE-20": "medium", "CWE-209": "medium"}
        tools = ["codeql", "semgrep"]

        for w in range(base_weakness_count):
            cwe = np.random.choice(available_cwes)
            sev = severities.get(cwe, "medium")
            wid = f"W-{weakness_counter:03d}"
            weakness_counter += 1
            line = np.random.randint(10, 200)
            file_path = f"src/main/java/com/thesis/securitystudy/service/{task}Service.java"

            # Each weakness found by 1-2 tools
            n_tools = np.random.choice([1, 2], p=[0.6, 0.4])
            selected_tools = np.random.choice(tools, size=n_tools, replace=False)

            for j, tool in enumerate(selected_tools):
                # Baseline findings
                findings_rows.append({
                    "finding_id": f"F-{finding_counter:03d}",
                    "pr_number": pr, "task_id": task, "condition": cond,
                    "ai_share": ai_share, "pipeline": "baseline",
                    "tool": tool, "rule_id": f"{tool}/{cwe.lower()}",
                    "cwe_id": cwe, "severity": sev,
                    "file_path": file_path, "start_line": line,
                    "fingerprint": f"fp-{finding_counter:03d}",
                    "triage_status": "TP",
                    "is_cross_tool_dup": j > 0,
                    "unique_weakness_id": wid,
                    "triage_notes": "demo data",
                })
                finding_counter += 1

                # Security-gated findings (some prevented)
                was_caught = np.random.random() < gate_catch_rate
                if not was_caught:
                    findings_rows.append({
                        "finding_id": f"F-{finding_counter:03d}",
                        "pr_number": pr, "task_id": task, "condition": cond,
                        "ai_share": ai_share, "pipeline": "security-gated",
                        "tool": tool, "rule_id": f"{tool}/{cwe.lower()}",
                        "cwe_id": cwe, "severity": sev,
                        "file_path": file_path, "start_line": line,
                        "fingerprint": f"fp-{finding_counter:03d}",
                        "triage_status": "TP",
                        "is_cross_tool_dup": j > 0,
                        "unique_weakness_id": wid,
                        "triage_notes": "demo data - escaped gate",
                    })
                    finding_counter += 1

    return pd.DataFrame(metrics_rows), pd.DataFrame(findings_rows)


if __name__ == "__main__":
    main()
