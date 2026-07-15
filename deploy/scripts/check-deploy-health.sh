#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_command docker
require_file .env
require_file docker-compose.yml
require_file backend/hive-backend.jar
require_file management-ui/dist/index.html
require_file nginx/conf.d/hive.conf
require_file nginx/certs/fullchain.pem
require_file nginx/certs/privkey.pem
require_file db-migrations/migration_manifest.txt
require_file scripts/migrate-db.sh

for key in MYSQL_ROOT_PASSWORD DB_APP_USERNAME DB_APP_PASSWORD AUTH_TOKEN_SECRET RESPONSE_ENCRYPT_KEY PRIVACY_HASH_SECRET EMPLOYEE_DEFAULT_PASSWORD TENANT_OWNER_DEFAULT_PASSWORD; do
  value="$(env_value "${key}")"
  [ -n "${value}" ] || fail ".env missing ${key}"
  case "${value}" in *CHANGE_ME*) fail ".env still contains a placeholder for ${key}" ;; esac
done

[ "$(find backend -maxdepth 1 -type f -name '*.jar' | wc -l | tr -d ' ')" = "1" ] || fail "backend directory must contain exactly one JAR"
docker compose config -q
grep -q '^  backend:$' docker-compose.yml || fail "Compose is missing the unified backend service"
grep -q 'container_name: hive-backend' docker-compose.yml || fail "Compose container identity is not hive-backend"
grep -q 'proxy_pass http://backend:8080' nginx/conf.d/hive.conf || fail "nginx does not route /api to backend:8080"
grep -q 'location /api/' nginx/conf.d/hive.conf || fail "nginx is missing /api routing"

bash scripts/inspect-backend-artifact.sh
echo "Deployment source health check passed."
