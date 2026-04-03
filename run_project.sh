#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ML_DIR="${ROOT_DIR}/ml"
ML_LOG="${ML_DIR}/ml_api.log"
ENV_FILE="${ROOT_DIR}/.env.local"
ENV_TEMPLATE="${ROOT_DIR}/.env.example"
SPRING_LOG="${ROOT_DIR}/project.log"
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

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "${value}"
}

check_command() {
  command -v "$1" >/dev/null 2>&1
}

ensure_env_file() {
  if [[ -f "${ENV_FILE}" ]]; then
    return
  fi

  if [[ -f "${ENV_TEMPLATE}" ]]; then
    cp "${ENV_TEMPLATE}" "${ENV_FILE}"
    echo "[RUN] Created .env.local from .env.example"
    return
  fi

  cat > "${ENV_FILE}" <<'EOF'
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/fraudshield
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
ML_API_URL=http://127.0.0.1:5000/predict
ML_HEALTH_URL=http://127.0.0.1:5000/health
EOF
  echo "[RUN] Created .env.local with local defaults"
}

load_env_file() {
  ensure_env_file

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

check_prerequisites() {
  local missing=0

  if ! check_command python3; then
    echo "[RUN] Missing required command: python3" >&2
    missing=1
  fi

  if ! check_command curl; then
    echo "[RUN] Missing required command: curl" >&2
    missing=1
  fi

  if [[ "${missing}" -ne 0 ]]; then
    exit 1
  fi
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

load_db_config() {
  DB_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://127.0.0.1:5432/fraudshield}"
  DB_USER="${SPRING_DATASOURCE_USERNAME:-postgres}"
  DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-postgres}"
  ML_HEALTH_CHECK_URL="${ML_HEALTH_URL:-http://127.0.0.1:5000/health}"

  if [[ -z "${DB_URL}" || -z "${DB_USER}" || -z "${DB_PASSWORD}" ]]; then
    echo "[RUN] Database settings are incomplete. Set SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD." >&2
    exit 1
  fi

  if [[ "${DB_PASSWORD}" == "your_password" ]]; then
    echo "[RUN] Replace SPRING_DATASOURCE_PASSWORD in .env.local before starting the project." >&2
    exit 1
  fi

  if [[ ! "${DB_URL}" =~ ^jdbc:postgresql://([^/:?]+)(:([0-9]+))?/([^?]+) ]]; then
    echo "[RUN] Unsupported spring.datasource.url format: ${DB_URL}" >&2
    echo "[RUN] Expected format: jdbc:postgresql://host:port/database?params" >&2
    exit 1
  fi

  DB_HOST="${BASH_REMATCH[1]}"
  if [[ -n "${BASH_REMATCH[3]:-}" ]]; then
    DB_PORT="${BASH_REMATCH[3]}"
  fi
  DB_NAME="${BASH_REMATCH[4]}"
  DB_NAME="${DB_NAME%%\?*}"
}

ensure_database_ready() {
  local db_exists

  echo "[RUN] Using database ${DB_NAME} on ${DB_HOST}:${DB_PORT} with user ${DB_USER}"

  if check_command pg_isready; then
    if ! PGPASSWORD="${DB_PASSWORD}" pg_isready -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" >/dev/null 2>&1; then
      echo "[RUN] PostgreSQL is not reachable at ${DB_HOST}:${DB_PORT} for user ${DB_USER}." >&2
      exit 1
    fi
  else
    echo "[RUN] pg_isready is not installed. Skipping PostgreSQL readiness check."
  fi

  if ! check_command psql; then
    echo "[RUN] psql is not installed. Skipping database existence check for ${DB_NAME}."
    return
  fi

  db_exists="$(PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" 2>/dev/null | tr -d '[:space:]')"
  if [[ "${db_exists}" == "1" ]]; then
    echo "[RUN] Database ${DB_NAME} already exists."
    return
  fi

  if ! check_command createdb; then
    echo "[RUN] Database ${DB_NAME} does not exist and createdb is not installed." >&2
    echo "[RUN] Create the database manually, then rerun." >&2
    exit 1
  fi

  echo "[RUN] Creating database ${DB_NAME}..."
  if ! PGPASSWORD="${DB_PASSWORD}" createdb -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" "${DB_NAME}" >/dev/null 2>&1; then
    echo "[RUN] Failed to create database ${DB_NAME}. Check credentials and privileges." >&2
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

  check_prerequisites
  mvn_cmd="$(pick_maven_cmd)"

  load_env_file
  load_db_config
  ensure_database_ready
  start_ml_if_needed
  pick_spring_port

  echo "[RUN] Starting Spring Boot app with ${mvn_cmd} on port ${SPRING_PORT}"
  echo "[RUN] Home URL: http://localhost:${SPRING_PORT}/pages/index.html"
  echo "[RUN] Dashboard URL: http://localhost:${SPRING_PORT}/pages/dashboard.html"
  echo "[RUN] Logs: ${SPRING_LOG}"
  cd "${ROOT_DIR}"
  SPRING_DATASOURCE_URL="${DB_URL}" \
  SPRING_DATASOURCE_USERNAME="${DB_USER}" \
  SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}" \
  "${mvn_cmd}" spring-boot:run -Dspring-boot.run.arguments="--server.port=${SPRING_PORT}" > "${SPRING_LOG}" 2>&1
}

main "$@"
