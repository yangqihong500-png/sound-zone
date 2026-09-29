# SoundZone repository instructions

## Release consistency

Read and apply the project-local `.codex/skills/soundzone-release-sync/SKILL.md` for every task that changes deployable SoundZone code and whenever the user asks to publish, deploy, sync, or make the public site current. This release policy is specific to the SoundZone repository and must not be applied to other projects.

For a completed implementation task, the standing project policy is to publish that task's verified deployable changes to `origin/main` and wait for the matching production workflow, unless the user says the work is local-only, a draft, or must not be deployed.

At the beginning of a code-changing task, record `HEAD`, branch or detached state, worktrees, and all pre-existing dirty paths. Treat those paths as belonging to the user or another conversation unless explicitly adopted into the current task.

Stage only files owned by the current task. In a dirty shared checkout, never use `git add -A`, `git add .`, broad staging globs, destructive resets, automatic stashing, or force-pushes. Stop before publication if another task edits the same path or the release cannot be separated safely.

Run checks appropriate to the changed frontend, backend, database, and deployment files. Do not publish failed tests, suspected secrets, unsafe migrations, or unresolved conflicts.

After pushing, verify the GitHub Actions production run whose `headSha` exactly matches the pushed commit. Before claiming the work is live, also verify the public homepage, `/healthz`, `/api/actuator/health`, and any feature-specific behavior relevant to the task.

For analysis, diagnosis, planning, or review without requested code changes, remain read-only. If publication is skipped or blocked, explicitly distinguish local changes from the GitHub and public versions.
