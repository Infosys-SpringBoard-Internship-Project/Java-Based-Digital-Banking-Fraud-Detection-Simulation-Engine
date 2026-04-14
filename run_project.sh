#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ML_DIR="${ROOT_DIR}/ml"
ML_LOG="${ML_DIR}/ml_api.log"
APP_PROPS="${ROOT_DIR}/src/main/resources/application.properties"
ENV_FILE="${ROOT_DIR}/.env.local"
ML_STARTED_BY_SCRIPT=0
ML_PID=""
SPRING_PORT=8080
DB_URL=""
DB_USER=""
DB_PASSWORD=""
DB_HOST=""
DB_PORT="5432"
DB_NAME=""
ML_HEALTH_CHECK_URL=""

cleanup() {
  if [[ "${ML_STARTED_BY_SCRIPT}" -eq 1 && -n "${ML_PID}" ]]; then
    echo "[RUN] Stopping ML API (pid=${ML_PID})"
    kill "${ML_PID}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT INT TERM

load_env_file() {
  if [[ ! -f "${ENV_FILE}" ]]; then
    return
  fi

  # shellcheck disable=SC2162
  while IFS= read line || [[ -n "${line}" ]]; do
    line="$(trim "${line}")"
    if [[ -z "${line}" || "${line}" == \#* ]]; then
      continue
    fi
    if [[ "${line}" != *=* ]]; then
      continue
    fi

    local key="${line%%=*}"
    local value="${line#*=}"
    key="$(trim "${key}")"
    value="$(trim "${value}")"
    value="${value%\"}"
    value="${value#\"}"

    if [[ -n "${key}" ]]; then
      export "${key}=${value}"
    fi
  done < "${ENV_FILE}"

  echo "[RUN] Loaded environment from .env.local"
}

pick_maven_cmd() {
  if [[ -x "${ROOT_DIR}/mvnw" && -f "${ROOT_DIR}/.mvn/wrapper/maven-wrapper.properties" ]]; then
    echo "${ROOT_DIR}/mvnw"
    return
  fi

  if command -v mvn >/dev/null 2>&1; then
    echo "mvn"
    return
  fi

  echo "[RUN] Maven is not available. Install Maven or restore ./mvnw." >&2
  exit 1
}

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "${value}"
}

read_property() {
  local key="$1"
  local line
  line="$(grep -E "^${key}=" "${APP_PROPS}" | tail -n 1 || true)"
  if [[ -z "${line}" ]]; then
    return 1
  fi
  trim "${line#*=}"
}

resolve_placeholder_default() {
  local value="$1"
  if [[ "${value}" =~ ^\$\{[^:}]+:(.*)\}$ ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
    return
  fi
  printf '%s' "${value}"
}

load_db_config() {
  if [[ ! -f "${APP_PROPS}" ]]; then
    echo "[RUN] Missing application.properties at ${APP_PROPS}" >&2
    exit 1
  fi

  DB_URL="${SPRING_DATASOURCE_URL:-$(read_property "spring.datasource.url" || true)}"
  DB_USER="${SPRING_DATASOURCE_USERNAME:-$(read_property "spring.datasource.username" || true)}"
  DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$(read_property "spring.datasource.password" || true)}"
  ML_HEALTH_CHECK_URL="${ML_HEALTH_URL:-$(read_property "ml.health.url" || true)}"

  DB_URL="$(resolve_placeholder_default "${DB_URL}")"
  DB_USER="$(resolve_placeholder_default "${DB_USER}")"
  DB_PASSWORD="$(resolve_placeholder_default "${DB_PASSWORD}")"
  ML_HEALTH_CHECK_URL="$(resolve_placeholder_default "${ML_HEALTH_CHECK_URL}")"

  if [[ -z "${ML_HEALTH_CHECK_URL}" ]]; then
    ML_HEALTH_CHECK_URL="http://127.0.0.1:5000/health"
  fi

  if [[ -z "${DB_URL}" || -z "${DB_USER}" || -z "${DB_PASSWORD}" ]]; then
    echo "[RUN] Database settings are incomplete. Set SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD." >&2
    exit 1
  fi

  if [[ "${DB_URL}" == *"your-host"* || "${DB_URL}" == *"your_database"* || "${DB_USER}" == "your_username" || "${DB_PASSWORD}" == "your_db_password" || "${DB_PASSWORD}" == "your_password" ]]; then
    echo "[RUN] Replace the placeholder database values in .env.local before starting the project." >&2
    exit 1
  fi

  if [[ ! "${DB_URL}" =~ ^jdbc:postgresql://([^/:?]+)(:([0-9]+))?/([^?]+)(\?.*)?$ ]]; then
    echo "[RUN] Unsupported spring.datasource.url format: ${DB_URL}" >&2
    echo "[RUN] Expected format: jdbc:postgresql://host:port/database?params" >&2
    exit 1
  fi

  DB_HOST="${BASH_REMATCH[1]}"
  if [[ -n "${BASH_REMATCH[3]:-}" ]]; then
    DB_PORT="${BASH_REMATCH[3]}"
  fi
  DB_NAME="${BASH_REMATCH[4]}"
}

check_command() {
  command -v "$1" >/dev/null 2>&1
}

ensure_database_ready() {
  local pg_ready_cmd=()
  local psql_cmd=()
  local createdb_cmd=()

  echo "[RUN] Using database ${DB_NAME} on ${DB_HOST}:${DB_PORT} with user ${DB_USER}"

  if ! check_command pg_isready; then
    echo "[RUN] pg_isready is not installed. Skipping PostgreSQL readiness check."
  else
    pg_ready_cmd=(pg_isready -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}")
    if ! PGPASSWORD="${DB_PASSWORD}" "${pg_ready_cmd[@]}" >/dev/null 2>&1; then
      echo "[RUN] PostgreSQL is not reachable at ${DB_HOST}:${DB_PORT} for user ${DB_USER}." >&2
      exit 1
    fi
  fi

  if ! check_command psql; then
    echo "[RUN] psql is not installed. Skipping database existence check for ${DB_NAME}."
    return
  fi

  psql_cmd=(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'")
  if [[ "$(PGPASSWORD="${DB_PASSWORD}" "${psql_cmd[@]}" 2>/dev/null | tr -d '[:space:]')" == "1" ]]; then
    echo "[RUN] Database ${DB_NAME} already exists."
    return
  fi

  if ! check_command createdb; then
    echo "[RUN] Database ${DB_NAME} does not exist and createdb is not installed." >&2
    echo "[RUN] Create the database manually, then rerun." >&2
    exit 1
  fi

  echo "[RUN] Creating database ${DB_NAME}..."
  createdb_cmd=(createdb -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" "${DB_NAME}")
  if ! PGPASSWORD="${DB_PASSWORD}" "${createdb_cmd[@]}"; then
    echo "[RUN] Failed to create database ${DB_NAME}." >&2
    exit 1
  fi
}

wait_for_ml() {
  local health_url="$1"
  local retries=30
  local i
  for ((i=1; i<=retries; i++)); do
    if curl -fsS "${health_url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

is_port_in_use() {
  local port="$1"
  python3 - "$port" <<'PY'
import socket
import sys

port = int(sys.argv[1])
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.settimeout(0.2)
rc = s.connect_ex(("127.0.0.1", port))
s.close()
sys.exit(0 if rc == 0 else 1)
PY
}

pick_spring_port() {
  if is_port_in_use "${SPRING_PORT}"; then
    local fallback_port=8081
    if [[ "${SPRING_PORT}" -eq 8081 ]]; then
      fallback_port=8082
    fi
    echo "[RUN] Port ${SPRING_PORT} is in use. Switching to ${fallback_port}."
    SPRING_PORT="${fallback_port}"
  fi
}

start_ml_if_needed() {
  if curl -fsS "${ML_HEALTH_CHECK_URL}" >/dev/null 2>&1; then
    echo "[RUN] ML API is healthy (${ML_HEALTH_CHECK_URL})."
    return
  fi

  if [[ "${ML_HEALTH_CHECK_URL}" != "http://127.0.0.1:5000/health" && "${ML_HEALTH_CHECK_URL}" != "http://localhost:5000/health" ]]; then
    echo "[RUN] Remote ML health check failed at ${ML_HEALTH_CHECK_URL}." >&2
    exit 1
  fi

  echo "[RUN] Starting ML API..."
  "${ML_DIR}/run_ml.sh" >"${ML_LOG}" 2>&1 &
  ML_PID="$!"
  ML_STARTED_BY_SCRIPT=1

  if ! wait_for_ml "${ML_HEALTH_CHECK_URL}"; then
    echo "[RUN] ML API failed to become healthy. Last log lines:"
    tail -n 40 "${ML_LOG}" || true
    exit 1
  fi

  echo "[RUN] ML API is healthy (http://127.0.0.1:5000/health)"
}

main() {
  local mvn_cmd
  mvn_cmd="$(pick_maven_cmd)"

  load_env_file
  load_db_config
  ensure_database_ready
  start_ml_if_needed
  pick_spring_port

  echo "[RUN] Starting Spring Boot app with ${mvn_cmd} on port ${SPRING_PORT}"
  echo "[RUN] Home URL: http://localhost:${SPRING_PORT}/pages/index.html"
  echo "[RUN] Dashboard URL: http://localhost:${SPRING_PORT}/pages/dashboard.html"
  cd "${ROOT_DIR}"
  "${mvn_cmd}" spring-boot:run -Dspring-boot.run.arguments="--server.port=${SPRING_PORT}"
}

main "$@"
