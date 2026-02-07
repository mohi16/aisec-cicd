#!/usr/bin/env python3
"""
AI Share Computation Tool

Computes AI Share for a pull request by comparing stored AI patches
against the final PR diff. AI Share = AI-originated lines / total lines added.

Usage:
    python compute_ai_share.py --pr 3 --patches-dir data/ai-patches --output data/pr-metrics.csv

The tool:
1. Reads all AI patch files for the given PR (pr-{N}-*.diff, pr-{N}-*.txt)
2. Reads the final diff (pr-{N}-final.diff)
3. Computes line-level overlap using fuzzy matching
4. Outputs AI Share (0.0–1.0)
"""

import argparse
import csv
import glob
import os
import re
import sys
from difflib import SequenceMatcher
from pathlib import Path


def extract_added_lines_from_diff(diff_text: str) -> list[str]:
    """Extract only added lines (starting with +) from a unified diff,
    excluding diff headers, empty lines, and import statements."""
    lines = []
    for line in diff_text.splitlines():
        if line.startswith("+++"):
            continue
        if line.startswith("+"):
            content = line[1:].strip()
            # Skip empty lines, pure imports, annotations-only, single braces
            if not content:
                continue
            if content.startswith("import "):
                continue
            if content.startswith("package "):
                continue
            if content in ("{", "}", ");", "});"):
                continue
            lines.append(content)
    return lines


def extract_code_lines_from_text(text: str) -> list[str]:
    """Extract code lines from a ChatGPT/Claude text export.
    Looks for code blocks (``` delimited) and extracts their content."""
    lines = []
    in_code_block = False

    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("```"):
            in_code_block = not in_code_block
            continue
        if in_code_block:
            # Skip trivial lines
            if not stripped:
                continue
            if stripped.startswith("import "):
                continue
            if stripped.startswith("package "):
                continue
            if stripped in ("{", "}", ");", "});"):
                continue
            lines.append(stripped)

    # If no code blocks found, treat entire content as potential code
    # (for raw Copilot suggestion files that are just code)
    if not lines:
        for line in text.splitlines():
            stripped = line.strip()
            if not stripped:
                continue
            if stripped.startswith("import "):
                continue
            if stripped.startswith("package "):
                continue
            if stripped in ("{", "}", ");", "});"):
                continue
            # Heuristic: line looks like code if it contains common patterns
            if any(kw in stripped for kw in [
                "public ", "private ", "protected ", "class ", "interface ",
                "return ", "if (", "for (", "while (", "try {", "catch (",
                "new ", "this.", "super.", "@", "=", "(", ".", ";"
            ]):
                lines.append(stripped)

    return lines


def similarity(a: str, b: str) -> float:
    """Compute similarity ratio between two strings."""
    return SequenceMatcher(None, a.strip(), b.strip()).ratio()


def compute_ai_share(
    ai_patches_dir: str,
    pr_number: int,
    similarity_threshold: float = 0.7,
    verbose: bool = False,
) -> dict:
    """Compute AI Share for a given PR.

    Returns dict with:
        - ai_share: float (0.0–1.0)
        - total_lines: int
        - ai_lines: int
        - matched_details: list of matched line pairs (for debugging)
    """
    patches_dir = Path(ai_patches_dir)

    # Load final diff
    final_diff_pattern = f"pr-{pr_number}-final.diff"
    final_diff_path = patches_dir / final_diff_pattern
    if not final_diff_path.exists():
        print(f"ERROR: Final diff not found: {final_diff_path}")
        sys.exit(1)

    final_lines = extract_added_lines_from_diff(final_diff_path.read_text())
    total_lines = len(final_lines)

    if total_lines == 0:
        return {
            "ai_share": 0.0,
            "total_lines": 0,
            "ai_lines": 0,
            "matched_details": [],
        }

    # Collect all AI-generated lines from patch files
    ai_lines_pool = []
    patch_patterns = [
        f"pr-{pr_number}-copilot-*.diff",
        f"pr-{pr_number}-copilot-*.txt",
        f"pr-{pr_number}-chatgpt-*.txt",
        f"pr-{pr_number}-claude-*.txt",
    ]

    for pattern in patch_patterns:
        for patch_file in sorted(patches_dir.glob(pattern)):
            text = patch_file.read_text()
            if patch_file.suffix == ".diff":
                extracted = extract_added_lines_from_diff(text)
            else:
                extracted = extract_code_lines_from_text(text)
            ai_lines_pool.extend(extracted)
            if verbose:
                print(f"  Loaded {len(extracted)} code lines from {patch_file.name}")

    if verbose:
        print(f"  Total AI lines pool: {len(ai_lines_pool)}")
        print(f"  Total final diff lines: {total_lines}")

    # Match: for each line in the final diff, check if it originated from AI
    matched_count = 0
    matched_details = []
    used_ai_indices = set()  # Prevent double-matching

    for final_line in final_lines:
        best_sim = 0.0
        best_idx = -1
        best_ai_line = ""

        for idx, ai_line in enumerate(ai_lines_pool):
            if idx in used_ai_indices:
                continue
            sim = similarity(final_line, ai_line)
            if sim > best_sim:
                best_sim = sim
                best_idx = idx
                best_ai_line = ai_line

        if best_sim >= similarity_threshold and best_idx >= 0:
            matched_count += 1
            used_ai_indices.add(best_idx)
            if verbose:
                matched_details.append({
                    "final_line": final_line,
                    "ai_line": best_ai_line,
                    "similarity": round(best_sim, 3),
                })

    ai_share = matched_count / total_lines if total_lines > 0 else 0.0

    return {
        "ai_share": round(ai_share, 4),
        "total_lines": total_lines,
        "ai_lines": matched_count,
        "matched_details": matched_details,
    }


def main():
    parser = argparse.ArgumentParser(description="Compute AI Share for a PR")
    parser.add_argument("--pr", type=int, required=True, help="PR number")
    parser.add_argument(
        "--patches-dir",
        default="data/ai-patches",
        help="Directory containing AI patches and final diffs",
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=0.7,
        help="Similarity threshold for line matching (default: 0.7)",
    )
    parser.add_argument("--verbose", action="store_true", help="Show match details")
    parser.add_argument(
        "--output",
        help="Append result to CSV file (creates if not exists)",
    )
    args = parser.parse_args()

    print(f"Computing AI Share for PR #{args.pr}...")
    result = compute_ai_share(
        ai_patches_dir=args.patches_dir,
        pr_number=args.pr,
        similarity_threshold=args.threshold,
        verbose=args.verbose,
    )

    print(f"\n{'='*50}")
    print(f"  PR #{args.pr}")
    print(f"  AI Share:     {result['ai_share']:.2%}")
    print(f"  AI lines:     {result['ai_lines']}")
    print(f"  Total lines:  {result['total_lines']}")
    print(f"{'='*50}")

    if args.verbose and result["matched_details"]:
        print("\nMatched lines (sample):")
        for detail in result["matched_details"][:10]:
            print(f"  [{detail['similarity']:.0%}] '{detail['final_line'][:60]}...'")
            print(f"       ← '{detail['ai_line'][:60]}...'")

    if args.output:
        file_exists = os.path.exists(args.output)
        with open(args.output, "a", newline="") as f:
            writer = csv.writer(f)
            if not file_exists:
                writer.writerow([
                    "pr_number", "ai_share", "ai_lines", "total_lines"
                ])
            writer.writerow([
                args.pr,
                result["ai_share"],
                result["ai_lines"],
                result["total_lines"],
            ])
        print(f"\nResult appended to {args.output}")


if __name__ == "__main__":
    main()
