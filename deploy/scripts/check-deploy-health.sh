#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

trim_value() {
  printf '%s' "$1" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

normalize_provider() {
  trim_value "$1" | tr '[:upper:]' '[:lower:]'
}

docker_available="YES"
if ! command -v docker >/dev/null 2>&1; then
  if [ "${ALLOW_MISSING_DOCKER:-NO}" = "YES" ]; then
    docker_available="NO"
    echo "WARN: Docker CLI unavailable; Compose expansion is deferred to the release host."
  else
    fail "missing command: docker"
  fi
fi
require_file .env
require_file docker-compose.yml
require_file backend/hive-backend.jar
require_file management-ui/dist/index.html
require_file nginx/conf.d/hive.conf
require_file nginx/certs/hellohive.top.pem
require_file nginx/certs/hellohive.top.key
require_file db-migrations/migration_manifest.txt
require_file db-migrations/migration_checksums.sha256
require_file db-migrations/baseline/hive_schema_baseline_v2.sql
require_file db-migrations/seeds/system_permission_catalog_v3.sql
require_file db-migrations/migrations/V20260710_001_installation_task_unique_key_repair.sql
require_file db-migrations/migrations/V20260710_003_builtin_role_permission_matrix.sql
require_file db-migrations/migrations/V20260710_004_order_role_status_scope.sql
require_file db-migrations/migrations/V20260715_001_order_notes_and_material_approval.sql
require_file scripts/migrate-db.sh
require_command sha256sum

database_mode="$(database_mode)"
database_mode="${database_mode:-COMPOSE}"
case "${database_mode}" in
  COMPOSE)
    database_required_keys=(MYSQL_ROOT_PASSWORD DB_APP_USERNAME DB_APP_PASSWORD)
    ;;
  EXTERNAL)
    database_required_keys=(HIVE_DATABASE_JDBC_URL HIVE_DATABASE_APP_USERNAME HIVE_DATABASE_APP_PASSWORD HIVE_DATABASE_ADMIN_HOST HIVE_DATABASE_ADMIN_PORT HIVE_DATABASE_ADMIN_USERNAME HIVE_DATABASE_ADMIN_PASSWORD)
    jdbc_url="$(env_value HIVE_DATABASE_JDBC_URL)"
    [[ "${jdbc_url}" == jdbc:mysql://* ]] || fail "EXTERNAL MySQL requires HIVE_DATABASE_JDBC_URL starting with jdbc:mysql://"
    admin_port="$(env_value HIVE_DATABASE_ADMIN_PORT)"
    [[ "${admin_port}" =~ ^[0-9]{1,5}$ ]] || fail "EXTERNAL MySQL requires a numeric HIVE_DATABASE_ADMIN_PORT"
    ;;
  *)
    fail "unsupported HIVE_DATABASE_MODE: ${database_mode} (use COMPOSE or EXTERNAL)"
    ;;
esac

for key in "${database_required_keys[@]}" AUTH_TOKEN_SECRET RESPONSE_ENCRYPT_KEY PRIVACY_HASH_SECRET EMPLOYEE_DEFAULT_PASSWORD TENANT_OWNER_DEFAULT_PASSWORD; do
  value="$(env_value "${key}")"
  [ -n "${value}" ] || fail ".env missing ${key}"
  case "${value}" in *CHANGE_ME*) fail ".env still contains a placeholder for ${key}" ;; esac
done

for weak_key in AUTH_TOKEN_SECRET RESPONSE_ENCRYPT_KEY PRIVACY_HASH_SECRET EMPLOYEE_DEFAULT_PASSWORD TENANT_OWNER_DEFAULT_PASSWORD; do
  value="$(env_value "${weak_key}")"
  case "${value}" in
    CHANGE_ME*|hive-backend-dev-token-secret|sygav9Iec4kZiRvivnwSVe3iWq66cTCleo8gr3qL2GyXTcQHXJ1E57ZqfhqfIyWp70Imy0rJ7ZkS5SI4T0asRQ==)
      fail ".env contains a public or placeholder value for ${weak_key}"
      ;;
  esac
done

cors_origins="$(env_value CORS_ALLOWED_ORIGINS)"
if echo "${cors_origins}" | grep -Eq '(^|,)[[:space:]]*\*[[:space:]]*(,|$)'; then
  fail "CORS_ALLOWED_ORIGINS must not contain a bare wildcard"
fi

if env_true NOTIFICATION_SMS_ENABLED; then
  for key in NOTIFICATION_SMS_ACCESS_KEY NOTIFICATION_SMS_ACCESS_SECRET NOTIFICATION_SMS_SIGN_NAME NOTIFICATION_SMS_TEMPLATE_CODE; do
    value="$(env_value "${key}")"
    [ -n "${value}" ] || fail "SMS is enabled but ${key} is empty"
    case "${value}" in CHANGE_ME*) fail "SMS is enabled but ${key} is a placeholder" ;; esac
  done
fi

logistics_provider="$(normalize_provider "$(env_value LOGISTICS_PROVIDER)")"
logistics_provider="${logistics_provider:-aliyun-market}"
case "${logistics_provider}" in
  apispace|aliyun-market) ;;
  *) fail "unsupported logistics provider: ${logistics_provider}" ;;
esac

if env_true APISPACE_LOGISTICS_ENABLED; then
  for key in APISPACE_LOGISTICS_TOKEN; do
    value="$(trim_value "$(env_value "${key}")")"
    [ -n "${value}" ] || fail "APISpace logistics is enabled but ${key} is empty"
    case "${value}" in CHANGE_ME*) fail "APISpace logistics is enabled but ${key} is a placeholder" ;; esac
  done
fi

if env_true ALIYUN_MARKET_LOGISTICS_ENABLED; then
  for key in ALIYUN_MARKET_LOGISTICS_APPCODE; do
    value="$(trim_value "$(env_value "${key}")")"
    [ -n "${value}" ] || fail "Aliyun Market logistics is enabled but ${key} is empty"
    case "${value}" in CHANGE_ME*) fail "Aliyun Market logistics is enabled but ${key} is a placeholder" ;; esac
  done
fi

storage_provider="$(normalize_provider "$(env_value FILE_STORAGE_PROVIDER)")"
storage_provider="${storage_provider:-local}"
case "${storage_provider}" in
  local) ;;
  aliyun-oss)
    env_true ALIYUN_OSS_ENABLED || fail "aliyun-oss storage requires ALIYUN_OSS_ENABLED=true"
    for key in ALIYUN_OSS_ENDPOINT ALIYUN_OSS_BUCKET ALIYUN_OSS_ACCESS_KEY_ID ALIYUN_OSS_ACCESS_KEY_SECRET; do
      value="$(trim_value "$(env_value "${key}")")"
      [ -n "${value}" ] || fail "aliyun-oss storage requires ${key}"
      case "${value}" in CHANGE_ME*) fail "aliyun-oss storage uses a placeholder for ${key}" ;; esac
    done
    ;;
  *) fail "unsupported file storage provider: ${storage_provider}" ;;
esac

[ "$(find backend -maxdepth 1 -type f -name '*.jar' | wc -l | tr -d ' ')" = "1" ] || fail "backend directory must contain exactly one JAR"
if [ "${docker_available}" = "YES" ]; then
  docker compose config -q
fi
grep -Eq '^  backend:[[:space:]]*$' docker-compose.yml || fail "Compose is missing the unified backend service"
grep -q 'container_name: hive-backend' docker-compose.yml || fail "Compose container identity is not hive-backend"
grep -q 'proxy_pass http://backend:8080' nginx/conf.d/hive.conf || fail "nginx does not route /api to backend:8080"
grep -q 'location /api/' nginx/conf.d/hive.conf || fail "nginx is missing /api routing"

(cd db-migrations && sha256sum -c migration_checksums.sha256 >/dev/null)

bash scripts/inspect-backend-artifact.sh
echo "Deployment source health check passed."
