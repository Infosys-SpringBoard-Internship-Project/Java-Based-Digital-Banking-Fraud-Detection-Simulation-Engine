#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ML_DIR="${ROOT_DIR}/ml"
ML_LOG="${ML_DIR}/ml_api.log"
APP_PROPS="${ROOT_DIR}/src/main/resources/application.properties"
ML_STARTED_BY_SCRIPT=0
ML_PID=""
SPRING_PORT=8080
DB_URL=""
DB_USER=""
DB_PASSWORD=""
DB_HOST=""
DB_PORT="5432"
DB_NAME=""

load_env_files() {
  local env_file
  for env_file in "${ROOT_DIR}/.env.local" "${ROOT_DIR}/.env"; do
    if [[ -f "${env_file}" ]]; then
      set -a
      # shellcheck disable=SC1090
      source "${env_file}"
      set +a
      echo "[RUN] Loaded environment from $(basename "${env_file}")"
      return
    fi
  done
}

cleanup() {
  if [[ "${ML_STARTED_BY_SCRIPT}" -eq 1 && -n "${ML_PID}" ]]; then
    echo "[RUN] Stopping ML API (pid=${ML_PID})"
    kill "${ML_PID}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT INT TERM

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

  DB_URL="${SPRING_DATASOURCE_URL:-}"
  DB_USER="${SPRING_DATASOURCE_USERNAME:-}"
  DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-}"

  if [[ -z "${DB_URL}" ]]; then
    DB_URL="$(read_property "spring.datasource.url" || true)"
  fi
  if [[ -z "${DB_USER}" ]]; then
    DB_USER="$(read_property "spring.datasource.username" || true)"
  fi
  if [[ -z "${DB_PASSWORD}" ]]; then
    DB_PASSWORD="$(read_property "spring.datasource.password" || true)"
  fi

  DB_URL="$(resolve_placeholder_default "${DB_URL}")"
  DB_USER="$(resolve_placeholder_default "${DB_USER}")"
  DB_PASSWORD="$(resolve_placeholder_default "${DB_PASSWORD}")"

  if [[ -z "${DB_URL}" || -z "${DB_USER}" || -z "${DB_PASSWORD}" ]]; then
    echo "[RUN] Database settings are incomplete. Set SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD." >&2
    exit 1
  fi

  if [[ "${DB_PASSWORD}" == "your_db_password" ]]; then
    echo "[RUN] Replace SPRING_DATASOURCE_PASSWORD with your Supabase DB password before starting the project." >&2
    exit 1
  fi

  local parsed_url="${DB_URL}"

  if [[ ! "${parsed_url}" =~ ^jdbc:postgresql://([^/:]+)(:([0-9]+))?/([^?]+)(\?.*)?$ ]]; then
    echo "[RUN] Unsupported spring.datasource.url format: ${DB_URL}" >&2
    echo "[RUN] Expected format: jdbc:postgresql://host:port/database" >&2
    exit 1
  fi

  DB_HOST="${BASH_REMATCH[1]}"
  if [[ -n "${BASH_REMATCH[3]:-}" ]]; then
    DB_PORT="${BASH_REMATCH[3]}"
  fi
  DB_NAME="${BASH_REMATCH[4]}"
}

ensure_database_ready() {
  echo "[RUN] Using database ${DB_NAME} on ${DB_HOST}:${DB_PORT} with user ${DB_USER}"

  if [[ "${DB_HOST}" != *"supabase.co" && "${DB_HOST}" != *"pooler.supabase.com" ]]; then
    echo "[RUN] This launcher is now Supabase-only." >&2
    echo "[RUN] Set SPRING_DATASOURCE_URL to your Supabase host (db.<project-ref>.supabase.co or pooler.supabase.com)." >&2
    exit 1
  fi

  echo "[RUN] Supabase host detected. Connection validity will be verified by Spring Boot startup."
}

wait_for_ml() {
  local retries=120
  local i
  for ((i=1; i<=retries; i++)); do
    if curl -fsS "http://127.0.0.1:5000/health" >/dev/null 2>&1; then
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
  if curl -fsS "http://127.0.0.1:5000/health" >/dev/null 2>&1; then
    echo "[RUN] ML API already running on port 5000. Reusing existing process."
    return
  fi

  echo "[RUN] Starting ML API..."
  "${ML_DIR}/run_ml.sh" >"${ML_LOG}" 2>&1 &
  ML_PID="$!"
  ML_STARTED_BY_SCRIPT=1

  if ! wait_for_ml; then
    echo "[RUN] ML API failed to become healthy. Last log lines:"
    tail -n 40 "${ML_LOG}" || true
    exit 1
  fi

  echo "[RUN] ML API is healthy (http://127.0.0.1:5000/health)"
}

main() {
  local mvn_cmd
  mvn_cmd="$(pick_maven_cmd)"

  load_env_files
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
