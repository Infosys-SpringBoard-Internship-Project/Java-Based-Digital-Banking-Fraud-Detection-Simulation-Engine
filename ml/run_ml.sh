#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="${SCRIPT_DIR}/.venv"
REQ_FILE="${SCRIPT_DIR}/requirements.txt"

if [[ ! -d "${VENV_DIR}" ]]; then
  echo "[ML] Creating virtual environment at ${VENV_DIR}"
  /usr/bin/python3 -m venv "${VENV_DIR}"
fi

if ! "${VENV_DIR}/bin/python" -m pip --version >/dev/null 2>&1; then
  echo "[ML] Existing virtual environment is broken. Recreating..."
  rm -rf "${VENV_DIR}"
  /usr/bin/python3 -m venv "${VENV_DIR}"
fi

# shellcheck disable=SC1091
source "${VENV_DIR}/bin/activate"

echo "[ML] Installing/updating dependencies"
"${VENV_DIR}/bin/python" -m pip install --upgrade pip
"${VENV_DIR}/bin/python" -m pip install -r "${REQ_FILE}"

echo "[ML] Starting Flask API on port 5000"
exec "${VENV_DIR}/bin/python" "${SCRIPT_DIR}/api/flask_api.py"
