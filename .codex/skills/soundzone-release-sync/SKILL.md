---
name: soundzone-release-sync
description: Safely finish and publish SoundZone code changes by reconciling the current checkout, GitHub main, Tencent Cloud deployment, and public health. Use after a SoundZone code-changing task or when asked to publish, deploy, sync, or make the public site current; distinguish local-only work and never mix unrelated dirty changes.
---

# SoundZone Release Sync

Keep one auditable release chain:

`scoped local changes -> verified commit -> origin/main -> exact GitHub Actions run -> healthy public site`

The project repository is `yangqihong500-png/sound-zone`. Discover current URLs and deployment configuration from the checkout and GitHub before acting; do not treat saved IPs, branch state, or previous run IDs as current truth.

## Standing release policy

For a completed SoundZone implementation task, publish the task's verified deployable changes automatically unless the user says the work is local-only, a draft, or must not be deployed. This standing policy covers the normal scoped `commit` and `push` needed to trigger the existing production workflow. It does not authorize publishing unrelated changes, resolving another task's conflicts, changing secrets or infrastructure, bypassing failed checks, force-pushing, or rewriting history.

If the task is analysis, diagnosis, planning, or review without requested code changes, remain read-only.

## Establish ownership before editing

At the beginning of a code-changing task:

1. Confirm the repository root, current branch or detached state, `HEAD`, worktree list, and `git status --short --branch`.
2. Fetch `origin/main` when network access is available and record whether the checkout is ahead, behind, or divergent.
3. Record all pre-existing modified and untracked paths. They belong to the user or another conversation unless the current user explicitly adopts them into this task.
4. Track the paths changed by the current task. If another task changes the same path concurrently, stop before staging or publishing and report the collision.

Managed worktrees are valid. Do not assume every conversation shares the primary checkout. The production source of truth is the commit ultimately pushed to `origin/main`.

## Verify the scoped change

Choose checks proportionate to the files changed:

- Backend: run Maven tests with Java 17 from `server/`.
- Frontend: run `npm run build:h5` from `app/`.
- Deployment files: validate shell syntax, Compose configuration, workflow YAML, and `git diff --check`.
- Database changes: inspect the migration, preserve existing data, and verify migration ordering and repeatability before release.
- Before staging, scan candidate files for private keys, access tokens, passwords, production `.env` files, and unexpectedly large artifacts.

Do not publish when required checks fail, credentials may be exposed, a migration is unsafe, or the release scope cannot be separated from unrelated work.

## Commit without capturing another conversation

Stage only the explicit files owned by the current task. Never use `git add -A`, `git add .`, or broad globs in a dirty shared checkout. Review both `git status --short` and `git diff --cached --stat`, then inspect sensitive or surprising staged changes before committing.

Immediately before pushing:

1. Fetch `origin/main` again.
2. Confirm the release commit contains only the intended task changes.
3. Require a fast-forward update to `main`.

If the remote advanced:

- In a clean isolated worktree, rebase the task commit onto `origin/main`, rerun affected checks, and push only if conflict-free.
- In a dirty shared checkout, do not stash, reset, or rewrite other work to make the push succeed. Stop and report the exact reconciliation needed.

Never force-push `main`.

## Follow the exact deployment

After pushing, find the GitHub Actions run whose `headSha` equals the pushed commit. Do not accept an older green run as evidence.

Wait for backend tests, frontend build, synchronization, container restart, and health checks to complete. On failure, inspect the failed log and make at most one low-risk, evidence-based repair and retry within the task's scope. Stop instead of repeatedly deploying when the cause is unclear, destructive, or requires new authority.

Use `scripts/check_release_state.sh` for a read-only parity snapshot when useful. It compares local and remote `main`, reports dirtiness, checks the latest matching workflow run when `gh` is available, and probes the public health endpoints.

## Prove public parity

Before saying the task is live, verify all applicable evidence:

- local release commit equals `origin/main`;
- the exact commit's production workflow concluded successfully;
- server containers are healthy when SSH access is available;
- the public homepage and `/healthz` respond successfully;
- `/api/actuator/health` reports `UP`;
- a feature-relevant public endpoint or observable UI state works when the task warrants it.

A successful local build, commit, push, or file upload alone does not prove public deployment.

## End-of-task report

Always state one of these outcomes plainly:

- **Published:** include commit SHA, Actions run URL, public URL, and verification results.
- **Local only:** identify uncommitted or unpushed paths and why publication was intentionally skipped.
- **Blocked:** identify the failed checkpoint and the safest next action.

Never imply that another conversation's uncommitted changes were included unless their paths were explicitly staged and verified.
