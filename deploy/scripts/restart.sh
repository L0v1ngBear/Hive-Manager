#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_file .env
bash scripts/normalize-env.sh .env
require_command docker
configure_profiles
prepare_runtime_directories

bash scripts/prune-management-ui.sh
bash scripts/check-deploy-health.sh

desired_hive_subnet="$(env_value HIVE_DOCKER_SUBNET)"
desired_hive_subnet="${desired_hive_subnet:-172.30.0.0/24}"
desired_network_hash="hive-net-v1:${desired_hive_subnet}"

resolve_compose_network_context() {
  local compose_config
  if ! compose_config="$(docker compose config --format json)"; then
    fail "无法读取 Docker Compose 渲染配置；请检查 .env、COMPOSE_PROJECT_NAME 和 docker-compose.yml。"
  fi

  compose_project_name="$(printf '%s\n' "${compose_config}" | sed -n -E 's/^[[:space:]]*"name"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p' | head -n 1)"
  compose_network_name="$(printf '%s\n' "${compose_config}" | awk '
    /^[[:space:]]*"networks"[[:space:]]*:/ {
      match($0, /^[[:space:]]*/)
      if (RLENGTH <= 2) {
        in_networks = 1
        networks_indent = RLENGTH
        next
      }
    }
    in_networks && /^[[:space:]]*"hive-net"[[:space:]]*:/ {
      match($0, /^[[:space:]]*/)
      if (RLENGTH > networks_indent) {
        in_hive_net = 1
        hive_net_indent = RLENGTH
        next
      }
    }
    in_hive_net && /^[[:space:]]*"name"[[:space:]]*:/ {
      match($0, /^[[:space:]]*/)
      if (RLENGTH > hive_net_indent) {
        line = $0
        sub(/^[^:]*:[[:space:]]*"/, "", line)
        sub(/".*$/, "", line)
        print line
        exit
      }
    }
  ')"

  [ -n "${compose_project_name}" ] || fail "无法从 Docker Compose 渲染配置解析项目名；请检查顶层 name 或 COMPOSE_PROJECT_NAME。"
  [ -n "${compose_network_name}" ] || fail "无法从 Docker Compose 渲染配置解析 hive-net 实际网络名。"
}

inspect_network_value() {
  local format="$1"
  local output
  if ! output="$(docker network inspect --format "${format}" "${compose_network_name}")"; then
    fail "无法检查 Docker 网络 ${compose_network_name}；未对服务执行变更，请先修复 Docker 权限或守护进程。"
  fi
  printf '%s\n' "${output}" | sed '/^[[:space:]]*$/d'
}

assert_hive_network_subnet() {
  local actual_subnet
  if ! docker network inspect "${compose_network_name}" >/dev/null 2>&1; then
    fail "启动后未找到 Docker 网络 ${compose_network_name}；请检查 docker compose up 输出。"
  fi
  actual_subnet="$(inspect_network_value '{{range .IPAM.Config}}{{if .Subnet}}{{.Subnet}}{{"\n"}}{{end}}{{end}}')"
  if [ "${actual_subnet}" != "${desired_hive_subnet}" ]; then
    fail "启动后 Docker 网络 ${compose_network_name} 子网为 '${actual_subnet:-未配置}'，期望 '${desired_hive_subnet}'；已停止健康检查，请修复网段冲突后重试。"
  fi
}

resolve_compose_network_context
network_restart_mode="absent"
if docker network inspect "${compose_network_name}" >/dev/null 2>&1; then
  attached_container_ids="$(inspect_network_value '{{range $id, $_ := .Containers}}{{$id}}{{"\n"}}{{end}}')"
  while IFS= read -r attached_container_id; do
    [ -n "${attached_container_id}" ] || continue
    if ! attached_project="$(docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}}' "${attached_container_id}")"; then
      fail "无法检查 Docker 网络 ${compose_network_name} 上的容器 ${attached_container_id}；未执行网络迁移。"
    fi
    if [ "${attached_project}" != "${compose_project_name}" ]; then
      fail "Docker 网络 ${compose_network_name} 连接了非当前 Compose 项目容器 ${attached_container_id}（项目：${attached_project:-unknown}）；请先确认并断开该 foreign container，再重试。"
    fi
  done <<< "${attached_container_ids}"

  actual_hive_subnet="$(inspect_network_value '{{range .IPAM.Config}}{{if .Subnet}}{{.Subnet}}{{"\n"}}{{end}}{{end}}')"
  actual_network_hash="$(inspect_network_value '{{index .Labels "com.hive.network.config-hash"}}')"
  if [ "${actual_hive_subnet}" = "${desired_hive_subnet}" ] && [ "${actual_network_hash}" = "${desired_network_hash}" ]; then
    network_restart_mode="matching"
  else
    network_restart_mode="migrate"
    echo "检测到 hive-net 需要迁移：当前子网='${actual_hive_subnet:-未配置}'，期望='${desired_hive_subnet}'，配置标记='${actual_network_hash:-legacy/missing}'。"
  fi
else
  if ! listed_networks="$(docker network ls --filter "name=^${compose_network_name}$" --format '{{.Name}}')"; then
    fail "无法列出 Docker 网络；未对服务执行变更，请先修复 Docker 权限或守护进程。"
  fi
  while IFS= read -r listed_network; do
    if [ "${listed_network}" = "${compose_network_name}" ]; then
      fail "Docker 网络 ${compose_network_name} 存在但无法 inspect；未对服务执行变更，请先检查 Docker 网络状态和权限。"
    fi
  done <<< "${listed_networks}"
fi

build_args=()
if [ "${PULL_IMAGES:-0}" = "1" ]; then
  docker compose pull nginx
  build_args+=(--pull)
fi
if [ "${NO_CACHE:-0}" = "1" ]; then
  build_args+=(--no-cache)
fi
docker compose build "${build_args[@]}" backend
docker compose stop backend || true

if ! bash scripts/migrate-db.sh; then
  echo "FAIL: database migration failed; backend remains stopped to prevent incompatible code from serving traffic." >&2
  echo "Inspect the migration error before starting the existing container manually." >&2
  exit 1
fi

if [ "${network_restart_mode}" = "migrate" ]; then
  echo "正在迁移 Compose 项目网络：所有当前启用的服务会短暂停机；不会删除 volumes 或持久化数据。"
  if ! docker compose down --remove-orphans; then
    fail "Docker Compose 网络迁移停止失败；未使用 -v/--volumes，持久化数据未被删除。请检查仍连接网络的容器后重试。"
  fi
  if ! docker compose up -d; then
    fail "Docker Compose 网络迁移重建失败；当前项目可能仍处于停机状态。未删除 volumes，请修复错误后运行 docker compose up -d。"
  fi
else
  docker compose up -d --force-recreate --remove-orphans backend nginx
fi

assert_hive_network_subnet
wait_for_healthy_container hive-backend
wait_for_healthy_container nginx
remove_retired_backend_containers
bash scripts/verify-release-integrity.sh
bash scripts/smoke-test.sh

echo "Unified backend restart completed."
