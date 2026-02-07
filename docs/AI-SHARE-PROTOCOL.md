# AI Share Measurement Protocol

## Overview

The study evaluates process-level effects of AI-assisted development
rather than inter-developer variability.

AI Share is measured per pull request as:

    AI Share = (lines in final diff attributable to AI) / (total lines added in final diff)

This is a continuous variable (0.0 to 1.0), also analyzed using three
protocol-defined conditions: Human-only, Low-AI, and High-AI.

---

## AI Tool Specification

A single AI tool and model are used throughout the entire experiment
to control for tool-level variability.

| Parameter | Value |
|-----------|-------|
| **Tool** | GitHub Copilot (inline suggestions + Copilot Chat in Ask mode) |
| **Model** | GPT-5 mini |
| **IDE** | IntelliJ IDEA / VS Code (whichever is primary) |
| **Copilot mode** | Ask mode (developer initiates requests; no autonomous edits) |
| **Model switching** | PROHIBITED during the experiment |
| **Other AI tools** | NOT used for code generation (no ChatGPT, no Claude chat, no other agents) |

**Rationale:** Using a single widely-available model ensures (a) consistent AI
capability across all PRs, (b) results that generalize to typical developer
workflows, and (c) a controlled independent variable (AI Share) without
confounding from tool differences. GPT-5 mini was chosen as a representative
fast-and-cost-efficient model available through Copilot at the time of the
experiment (February 2026).

**Note:** Claude Chat / Claude Code may still be used for non-code tasks
(e.g., debugging pipeline issues, running analysis scripts, writing the
thesis document). These uses do NOT count as AI code generation and are
not captured as AI patches.

---

## Condition Definitions

### Human-only (target AI Share: 0.0)

**Protocol:**
- GitHub Copilot is DISABLED (plugin toggled off in IDE)
- No AI tools used for code generation of any kind
- AI may be used for non-code tasks only (e.g., reading documentation)
- IDE autocompletion (IntelliJ built-in) is allowed — this is not AI assistance
- Developer writes all code manually

**Verification:**
- Copilot plugin disabled (screenshot IDE settings before starting)
- No AI patch files exist for this PR

### Low-AI (AI used as assistant)

**Protocol:**
- GitHub Copilot is ENABLED
- Developer writes core logic, security-relevant code, and architectural decisions manually
- Copilot may be used for: boilerplate, getters/setters, standard patterns, test scaffolding
- Developer critically reviews every Copilot suggestion before accepting
- Developer makes all security decisions independently
- Copilot Chat may be used to ask "how do I do X" but not to generate entire methods

### High-AI (AI as primary author)

**Protocol:**
- GitHub Copilot is ENABLED and used as the primary code generation method
- Developer writes comments/prompts, Copilot generates the implementation
- Copilot Chat used freely to generate larger code blocks
- Developer reviews for correctness and makes minimal edits for integration
- Developer does NOT rewrite security-relevant sections unless AI output is clearly broken
- Goal: let the AI do most of the work, as a developer under time pressure might

---

## Capturing AI Output (Source of Truth)

For AI Share to be reproducible, the raw AI output must be recoverable
from the git history. The primary method is **AI snapshot commits**.

### Primary Method: AI Snapshot Commits

For each AI contribution, use a two-commit workflow:

1. **Accept the AI output as-is** → commit with message `"AI snapshot: <description>"`
2. **Make your edits** (fixes, integration, security changes) → commit with message `"Final: <description>"`

AI Share is then computed by comparing the snapshot commit against the
final commit. This is fully reproducible from git history alone.

**Rules per condition:**

- **Human-only:** No AI snapshot commits. All commits are regular commits.
- **Low-AI:** AI snapshot commits only for boilerplate, patterns, and test
  scaffolding. Core logic and security-relevant code must appear in a
  non-snapshot (regular or "Final") commit, written manually.
- **High-AI:** At least one AI snapshot commit per task (the first complete
  draft). Then one "Final" commit with minimal integration edits.

**Example workflow:**
```
# Copilot generates SearchService.java
# Accept it exactly as generated → stage and commit
git add src/main/java/.../service/SearchService.java
git commit -m "AI snapshot: SearchService from Copilot"

# Now edit it (fix imports, adjust logic, etc.) → commit
git add src/main/java/.../service/SearchService.java
git commit -m "Final: SearchService with manual edits"
```

For inline suggestions that build up a file gradually, batch them:
commit the AI snapshot once you've accepted a logical block of suggestions,
before you start manually editing.

### Supplementary: Copilot Chat Exports

For larger code blocks generated via Copilot Chat, also save the
full chat response as supporting evidence:

| Source | File Naming |
|--------|-------------|
| Copilot Chat response | `pr-{N}-copilot-chat-{SEQ}.txt` |

Store in `data/ai-patches/` on the `results` branch. These are
supplementary — the AI snapshot commits are the primary evidence.

### Session Log

The session log provides context but is NOT the primary measurement input.
Keep `session-log.csv` **outside the repo directory** (or add it to
`.gitignore`) so it never enters a PR diff.

Record events in the log:

| Event Type | When to Log |
|------------|-------------|
| `session_start` | Beginning work on a PR |
| `ai_snapshot_committed` | Committing an AI snapshot |
| `copilot_suggestion_rejected` | Dismissing a significant Copilot suggestion |
| `copilot_small_completions` | Batch entry: "~N lines of small Copilot completions" |
| `manual_code_written` | Writing a significant block manually |
| `session_end` | Finishing work on a PR |

---

## Computing AI Share

### Step 1: Extract AI-originated lines

For each PR branch, compare AI snapshot commits against final commits:

```bash
# List AI snapshot commits on the branch
git log --oneline scaffold-v1..HEAD --grep="AI snapshot"

# For each snapshot commit, diff it against its corresponding final commit
git diff <snapshot-sha> <final-sha> -- <file>
```

Lines that survive unchanged from an AI snapshot into the final state
are "AI-originated". Lines added or substantially changed in final
commits are "human-originated".

A line is "AI-originated" if:
- It appears in an AI snapshot commit AND in the final branch state (exact or near-match, >=70% similarity)
- It was generated by Copilot and kept with minor edits (>50% of content preserved)

A line is NOT "AI-originated" if:
- It was in an AI snapshot but completely rewritten in a final commit (>50% changed)
- It is a standard import/annotation that any developer would write identically

### Step 2: Calculate

```
ai_lines = lines in final diff (vs scaffold) that trace back to AI snapshots
total_lines_added = total added lines in final diff (git diff --stat scaffold-v1..HEAD)
AI Share = ai_lines / total_lines_added
```

### Step 3: Record

Store in `data/pr-metrics.csv` on the `results` branch:
- `ai_share_measured`: the computed value (0.0-1.0)
- AI snapshot commits remain in the branch history for independent verification

### Edge Cases

- **Empty lines, imports auto-organized by IDE, formatting-only changes:** Exclude from both numerator and denominator
- **AI generated an entire file, developer made 5 edits:** Count file lines minus edited lines as AI-originated
- **Developer typed code inspired by AI explanation (no copy-paste):** Not AI-originated

---

## Validation

Expected ranges (not requirements):

- Human-only PRs: AI Share = 0.0
- Low-AI PRs: typically 0.15-0.45
- High-AI PRs: typically 0.60-0.95

If a PR falls outside its expected range, report the actual value as-is.

---

## Reproducibility Guarantee

Any independent reviewer can:

1. Check out any task branch
2. Identify AI snapshot commits via `git log --grep="AI snapshot"`
3. Diff snapshots against final commits to determine AI-originated lines
4. Recompute AI Share using the matching rules above
5. Compare with the reported value

All evidence lives in the git history — no external files required.
Supplementary Copilot Chat exports and session logs provide additional
context but are not the primary evidence source.
