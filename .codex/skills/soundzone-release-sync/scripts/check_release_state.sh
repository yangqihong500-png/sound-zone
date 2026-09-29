#!/usr/bin/env bash
set -u

repo_path="${1:-.}"
public_url="${SOUNDZONE_PUBLIC_URL:-http://42.193.189.68}"
repo_name="${SOUNDZONE_GITHUB_REPO:-yangqihong500-png/sound-zone}"

cd "$repo_path" || exit 2
root="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "status=error reason=not-a-git-repository"
  exit 2
}
cd "$root" || exit 2

local_sha="$(git rev-parse HEAD)"
branch="$(git branch --show-current)"
dirty_count="$(git status --porcelain | wc -l | tr -d ' ')"
remote_source="cached"
remote_sha=""
if command -v gh >/dev/null 2>&1; then
  remote_sha="$(gh api "repos/$repo_name/git/ref/heads/main" --jq .object.sha 2>/dev/null || true)"
  remote_source="github"
fi
if [[ -z "$remote_sha" ]]; then
  remote_sha="$(git rev-parse refs/remotes/origin/main 2>/dev/null || true)"
  remote_source="cached"
fi

echo "root=$root"
echo "branch=${branch:-detached}"
echo "local_sha=$local_sha"
echo "remote_main_sha=${remote_sha:-unavailable}"
echo "remote_source=$remote_source"
echo "dirty_paths=$dirty_count"

result=0
if [[ -n "$remote_sha" && "$local_sha" != "$remote_sha" ]]; then
  echo "git_parity=mismatch"
  result=1
else
  echo "git_parity=match"
fi

if command -v gh >/dev/null 2>&1 && [[ -n "$remote_sha" ]]; then
  run_json="$(gh run list -R "$repo_name" --workflow production.yml --commit "$remote_sha" --limit 1 --json status,conclusion,headSha,url 2>/dev/null || true)"
  if [[ "$run_json" == "[]" || -z "$run_json" ]]; then
    deployable_change="$(git diff-tree --no-commit-id --name-only -r "$remote_sha" 2>/dev/null | grep -E '^(app/|server/|deploy/|docker-compose\.prod\.yml$|\.env\.production\.example$|\.github/workflows/production\.yml$)' || true)"
    if [[ -n "$deployable_change" ]]; then
      echo "workflow=missing-for-deployable-head"
      result=1
    else
      echo "workflow=not-required-for-config-only-head"
    fi
  else
    echo "workflow=$run_json"
    if ! printf '%s' "$run_json" | grep -q '"conclusion":"success"'; then
      result=1
    fi
  fi
else
  echo "workflow=not-checked"
fi

healthz="$(curl --fail --silent --show-error --max-time 10 "$public_url/healthz" 2>/dev/null || true)"
backend="$(curl --fail --silent --show-error --max-time 10 "$public_url/api/actuator/health" 2>/dev/null || true)"

echo "public_url=$public_url"
echo "healthz=${healthz:-unavailable}"
echo "backend_health=${backend:-unavailable}"

if [[ "$healthz" != "ok" ]] || ! printf '%s' "$backend" | grep -q '"status":"UP"'; then
  result=1
fi

exit "$result"
