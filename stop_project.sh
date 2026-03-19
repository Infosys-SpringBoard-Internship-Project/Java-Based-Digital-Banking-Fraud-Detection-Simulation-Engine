#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ML_LOG="${ROOT_DIR}/ml/ml_api.log"

stop_port() {
  local port="$1"
  local name="$2"
  local pids
  pids="$(lsof -ti tcp:"${port}" || true)"

  if [[ -z "${pids}" ]]; then
    echo "[STOP] ${name} not running on port ${port}."
    return
  fi

  echo "[STOP] Stopping ${name} on port ${port} (pid: ${pids//$'\n'/, })"
  kill ${pids} >/dev/null 2>&1 || true
}

stop_spring_boot() {
  local pids
  pids="$(pgrep -f "org.springframework.boot.loader|spring-boot:run|infosys_project" || true)"
  if [[ -n "${pids}" ]]; then
    echo "[STOP] Stopping Spring Boot process(es): ${pids//$'\n'/, }"
    kill ${pids} >/dev/null 2>&1 || true
  fi
}

stop_ml_processes() {
  local pids
  pids="$(pgrep -f "ml/run_ml.sh|ml/api/flask_api.py|flask_api.py" || true)"
  if [[ -n "${pids}" ]]; then
    echo "[STOP] Stopping ML process(es): ${pids//$'\n'/, }"
    kill ${pids} >/dev/null 2>&1 || true
  fi
}

main() {
  stop_port 8080 "Spring Boot"
  stop_port 8081 "Spring Boot"
  stop_port 8082 "Spring Boot"
  stop_port 5000 "ML API"

  stop_spring_boot
  stop_ml_processes

  if [[ -f "${ML_LOG}" ]]; then
    echo "[STOP] Latest ML log: ${ML_LOG}"
  fi

  echo "[STOP] Done."
}

main "$@"
