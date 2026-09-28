#!/bin/sh
set -eu

: "${MYSQL_HOST:?MYSQL_HOST is required}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
: "${MYSQL_USER:?MYSQL_USER is required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"

export MYSQL_PWD="$MYSQL_PASSWORD"

mysql_cmd() {
  mysql --protocol=tcp -h "$MYSQL_HOST" -P "${MYSQL_PORT:-3306}" -u "$MYSQL_USER" "$MYSQL_DATABASE" "$@"
}

attempt=0
until mysql_cmd -Nse 'SELECT 1' >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    echo 'MySQL did not become ready in time.' >&2
    exit 1
  fi
  sleep 2
done

table_count=$(mysql_cmd -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='sz_user'")

if [ "$table_count" = '0' ]; then
  echo 'Initializing a new SoundZone database from schema.sql.'
  # schema.sql contains a local-development CREATE DATABASE/USE preamble. The
  # production database is selected by mysql_cmd, so remove that preamble.
  sed '/^CREATE DATABASE IF NOT EXISTS soundzone$/,/^USE soundzone;$/d' /sql/schema.sql | mysql_cmd

  mysql_cmd <<'SQL'
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY,
  applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SQL

  for file in /sql/migrations/V*.sql; do
    [ -f "$file" ] || continue
    version=$(basename "$file" | cut -d_ -f1)
    mysql_cmd -e "INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('$version',NOW(6));"
  done
  echo 'Database baseline initialized.'
  exit 0
fi

mysql_cmd <<'SQL'
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY,
  applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SQL

pending=''
for file in /sql/migrations/V*.sql; do
  [ -f "$file" ] || continue
  version=$(basename "$file" | cut -d_ -f1)
  applied=$(mysql_cmd -Nse "SELECT COUNT(*) FROM sz_schema_migration WHERE version='$version'")
  if [ "$applied" = '0' ]; then
    pending="$pending $file"
  fi
done

if [ -z "$pending" ]; then
  echo 'Database schema is already current.'
  exit 0
fi

mkdir -p /backups
backup="/backups/pre-migrate-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
echo "Creating migration backup: $backup"
mysqldump --protocol=tcp -h "$MYSQL_HOST" -P "${MYSQL_PORT:-3306}" -u "$MYSQL_USER" \
  --single-transaction --routines --triggers "$MYSQL_DATABASE" | gzip > "$backup"

for file in $pending; do
  version=$(basename "$file" | cut -d_ -f1)
  echo "Applying $version from $(basename "$file")"
  mysql_cmd < "$file"
done

echo 'All pending database migrations were applied.'
