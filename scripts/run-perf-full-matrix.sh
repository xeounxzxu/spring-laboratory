#!/usr/bin/env bash
set -euo pipefail

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 CLI not found. Install via https://k6.io/docs/get-started/installation/ or 'brew install k6'." >&2
  exit 1
fi

if ! command -v wrk >/dev/null 2>&1; then
  echo "wrk CLI not found. Install from https://github.com/wg/wrk or 'brew install wrk'." >&2
  exit 1
fi

REPO_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
APP_DIR="$REPO_DIR/mvc/coroutine-app"

PORT=${PORT:-8082}
TOMCAT_MAX_MATRIX=${TOMCAT_MAX_MATRIX:-"50 200"}
ENDPOINT_MATRIX=${ENDPOINT_MATRIX:-"blocking suspend io-blocking io-suspend"}
SLEEP_MATRIX=${SLEEP_MATRIX:-"10 100 300"}

K6_DURATION=${K6_DURATION:-10s}
K6_RATE_MATRIX=${K6_RATE_MATRIX:-"120 300 600"}
K6_PRE_ALLOCATED_VUS=${K6_PRE_ALLOCATED_VUS:-80}
K6_MAX_VUS=${K6_MAX_VUS:-600}

WRK_DURATION=${WRK_DURATION:-10s}
WRK_THREADS=${WRK_THREADS:-8}
WRK_CONNECTION_MATRIX=${WRK_CONNECTION_MATRIX:-"120 300"}
WRK_TIMEOUT=${WRK_TIMEOUT:-5s}
RUN_TAG=${RUN_TAG:-$(date +%Y%m%d-%H%M%S)}
K6_ROOT_DIR=${K6_ROOT_DIR:-$REPO_DIR/load/k6/results/$RUN_TAG}
WRK_ROOT_DIR=${WRK_ROOT_DIR:-$REPO_DIR/load/wrk/results/$RUN_TAG}
REPORT_FILE=${REPORT_FILE:-$REPO_DIR/load/reports/perf-summary-$RUN_TAG.md}

APP_PID=""

cleanup() {
  if [[ -n "${APP_PID:-}" ]] && kill -0 "$APP_PID" >/dev/null 2>&1; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

wait_for_app() {
  local max_retries=60
  local attempt=1
  local url="http://localhost:${PORT}/perf/blocking?sleepMs=1"

  while (( attempt <= max_retries )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
    ((attempt++))
  done
  return 1
}

echo "Building coroutine-app bootJar"
./gradlew :mvc:coroutine-app:bootJar >/dev/null

for tomcat_max in $TOMCAT_MAX_MATRIX; do
  echo ""
  echo "=== TOMCAT_MAX=$tomcat_max ==="

  cleanup

  SERVER_PORT="$PORT" \
  SERVER_TOMCAT_THREADS_MAX="$tomcat_max" \
  SERVER_TOMCAT_THREADS_MIN_SPARE="$tomcat_max" \
  APP_PERF_MOCK_BASE_URL="http://localhost:${PORT}/mock" \
  java -jar "$APP_DIR/build/libs/coroutine-app-0.0.1-SNAPSHOT.jar" \
    > "$REPO_DIR/load/perf-app-tomcat${tomcat_max}.log" 2>&1 &
  APP_PID=$!

  if ! wait_for_app; then
    echo "Application did not become ready for TOMCAT_MAX=$tomcat_max" >&2
    exit 1
  fi

  BASE_URL="http://localhost:${PORT}" \
  DURATION="$K6_DURATION" \
  RATE_MATRIX="$K6_RATE_MATRIX" \
  PRE_ALLOCATED_VUS="$K6_PRE_ALLOCATED_VUS" \
  MAX_VUS="$K6_MAX_VUS" \
  ENDPOINT_MATRIX="$ENDPOINT_MATRIX" \
  SLEEP_MATRIX="$SLEEP_MATRIX" \
  OUT_DIR="$K6_ROOT_DIR/tomcat-max${tomcat_max}" \
  "$REPO_DIR/scripts/run-k6-perf-matrix.sh"

  BASE_URL="http://localhost:${PORT}" \
  THREADS="$WRK_THREADS" \
  CONNECTION_MATRIX="$WRK_CONNECTION_MATRIX" \
  DURATION="$WRK_DURATION" \
  TIMEOUT="$WRK_TIMEOUT" \
  ENDPOINT_MATRIX="$ENDPOINT_MATRIX" \
  SLEEP_MATRIX="$SLEEP_MATRIX" \
  OUT_DIR="$WRK_ROOT_DIR/tomcat-max${tomcat_max}" \
  "$REPO_DIR/scripts/run-wrk-perf-matrix.sh"
done

K6_DIR="$K6_ROOT_DIR" WRK_DIR="$WRK_ROOT_DIR" OUT_FILE="$REPORT_FILE" \
  "$REPO_DIR/scripts/generate-perf-report.sh"
echo "Full matrix run complete."
