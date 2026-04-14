#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="${SCRIPT_DIR}/.venv"
REQ_FILE="${SCRIPT_DIR}/requirements.txt"
PYTHON_CMD="${PYTHON_CMD:-}"

resolve_executable() {
  local candidate="$1"

  if [[ -z "${candidate}" ]]; then
    return 1
  fi

  if [[ -x "${candidate}" ]]; then
    printf '%s\n' "${candidate}"
    return 0
  fi

  if command -v "${candidate}" >/dev/null 2>&1; then
    command -v "${candidate}"
    return 0
  fi

  return 1
}

python_version_supported() {
  local candidate="$1"
  "${candidate}" - <<'PY' >/dev/null 2>&1
import sys

sys.exit(0 if sys.version_info >= (3, 10) else 1)
PY
}

pick_python_cmd() {
  local candidate=""
  local resolved=""

  for candidate in "${PYTHON_CMD:-}" python3 python; do
    resolved="$(resolve_executable "${candidate}" || true)"
    if [[ -z "${resolved}" ]]; then
      continue
    fi

    if python_version_supported "${resolved}"; then
      echo "${resolved}"
      return
    fi
  done

  echo "[ML] Python 3.10+ is required. Install python3 or python, or set PYTHON_CMD to a compatible interpreter." >&2
  exit 1
}

PYTHON_CMD="$(pick_python_cmd)"
echo "[ML] Using Python interpreter: ${PYTHON_CMD}"

if [[ ! -d "${VENV_DIR}" ]]; then
  echo "[ML] Creating virtual environment at ${VENV_DIR}"
  "${PYTHON_CMD}" -m venv "${VENV_DIR}"
fi

if ! "${VENV_DIR}/bin/python" -m pip --version >/dev/null 2>&1; then
  echo "[ML] Existing virtual environment is broken. Recreating..."
  rm -rf "${VENV_DIR}"
  "${PYTHON_CMD}" -m venv "${VENV_DIR}"
fi

# shellcheck disable=SC1091
source "${VENV_DIR}/bin/activate"

echo "[ML] Installing/updating dependencies"
"${VENV_DIR}/bin/python" -m pip install --upgrade pip
"${VENV_DIR}/bin/python" -m pip install -r "${REQ_FILE}"

echo "[ML] Starting Flask API on port 5000"
exec "${VENV_DIR}/bin/python" "${SCRIPT_DIR}/api/flask_api.py"
