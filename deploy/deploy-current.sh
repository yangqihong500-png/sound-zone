#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
config_file="${SOUNDZONE_DEPLOY_CONFIG:-$repo_root/.deploy.production.local}"

if [[ ! -f "$config_file" ]]; then
  echo "Missing deployment config: $config_file" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$config_file"

: "${DEPLOY_HOST:?set DEPLOY_HOST in $config_file}"
: "${DEPLOY_PORT:?set DEPLOY_PORT in $config_file}"
: "${DEPLOY_USER:?set DEPLOY_USER in $config_file}"
: "${DEPLOY_PATH:?set DEPLOY_PATH in $config_file}"
: "${DEPLOY_SSH_KEY:?set DEPLOY_SSH_KEY in $config_file}"

known_hosts_file="${DEPLOY_KNOWN_HOSTS_FILE:-$repo_root/.deploy-known-hosts}"
ssh_command="ssh -i $DEPLOY_SSH_KEY -p $DEPLOY_PORT -o UserKnownHostsFile=$known_hosts_file -o StrictHostKeyChecking=yes"

rsync -az --delete \
  --exclude '.git/' \
  --exclude '.env.production' \
  --exclude '.deploy.production.local' \
  --exclude '.deploy-known-hosts' \
  --exclude 'app/node_modules/' \
  --exclude 'app/dist/' \
  --exclude 'server/target/' \
  --exclude 'data/' \
  --exclude 'backups/' \
  -e "$ssh_command" \
  "$repo_root/" "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_PATH/"

ssh -i "$DEPLOY_SSH_KEY" \
  -p "$DEPLOY_PORT" \
  -o UserKnownHostsFile="$known_hosts_file" \
  -o StrictHostKeyChecking=yes \
  "$DEPLOY_USER@$DEPLOY_HOST" \
  "set -eu; cd '$DEPLOY_PATH'; \
   mkdir -p data/images data/audio backups; \
   sudo docker compose --env-file .env.production -f docker-compose.prod.yml build server; \
   sudo docker compose --env-file .env.production -f docker-compose.prod.yml build web; \
   sudo docker compose --env-file .env.production -f docker-compose.prod.yml up -d --remove-orphans; \
   curl --fail --retry 12 --retry-delay 5 --retry-all-errors http://127.0.0.1/healthz; \
   sudo docker compose --env-file .env.production -f docker-compose.prod.yml ps"

echo
echo "Deployment complete: http://$DEPLOY_HOST"
