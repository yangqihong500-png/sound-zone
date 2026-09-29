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
remote_sha="$(git ls-remote origin refs/heads/main 2>/dev/null | awk 'NR == 1 {print $1}')"

echo "root=$root"
echo "branch=${branch:-detached}"
echo "local_sha=$local_sha"
echo "remote_main_sha=${remote_sha:-unavailable}"
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
    echo "workflow=unavailable"
    result=1
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
