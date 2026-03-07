#!/usr/bin/env bash
set -euo pipefail

if ! command -v wrk >/dev/null 2>&1; then
  echo "wrk CLI not found. Install from https://github.com/wg/wrk or 'brew install wrk'." >&2
  exit 1
fi

REPO_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
BASE_URL=${BASE_URL:-http://localhost:8082}
THREADS=${THREADS:-8}
CONNECTIONS=${CONNECTIONS:-200}
CONNECTION_MATRIX=${CONNECTION_MATRIX:-$CONNECTIONS}
DURATION=${DURATION:-30s}
TIMEOUT=${TIMEOUT:-5s}
SLEEP_MATRIX=${SLEEP_MATRIX:-"10 50 100 300"}
ENDPOINT_MATRIX=${ENDPOINT_MATRIX:-"blocking suspend"}
OUT_DIR=${OUT_DIR:-$REPO_DIR/load/wrk/results}

mkdir -p "$OUT_DIR"

resolve_path() {
  case "$1" in
    blocking) echo "/perf/blocking" ;;
    suspend) echo "/perf/suspend" ;;
    io-blocking) echo "/perf-io/blocking" ;;
    io-suspend) echo "/perf-io/suspend" ;;
    *) echo "/perf/blocking" ;;
  esac
}

echo "Running wrk matrix against $BASE_URL (-t$THREADS -c[$CONNECTION_MATRIX] -d$DURATION --timeout $TIMEOUT)"

for conn in $CONNECTION_MATRIX; do
  for endpoint in $ENDPOINT_MATRIX; do
    for sleep_ms in $SLEEP_MATRIX; do
      path=$(resolve_path "$endpoint")
      url="${BASE_URL}${path}?sleepMs=${sleep_ms}"
      out_file="$OUT_DIR/${endpoint}-sleep${sleep_ms}-conn${conn}-thr${THREADS}.txt"
      echo ""
      echo "=== conn=$conn endpoint=$endpoint sleepMs=$sleep_ms ==="
      wrk -t"$THREADS" -c"$conn" -d"$DURATION" --timeout "$TIMEOUT" --latency "$url" | tee "$out_file"
    done
  done
done

echo ""
echo "Saved outputs to $OUT_DIR"
