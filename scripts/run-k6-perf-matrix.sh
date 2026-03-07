#!/usr/bin/env bash
set -euo pipefail

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 CLI not found. Install via https://k6.io/docs/get-started/installation/ or 'brew install k6'." >&2
  exit 1
fi

REPO_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
SCRIPT_FILE=${SCRIPT_FILE:-$REPO_DIR/load/k6/perf-comparison.js}
BASE_URL=${BASE_URL:-http://localhost:8082}
DURATION=${DURATION:-30s}
RATE=${RATE:-200}
RATE_MATRIX=${RATE_MATRIX:-$RATE}
PRE_ALLOCATED_VUS=${PRE_ALLOCATED_VUS:-50}
MAX_VUS=${MAX_VUS:-400}
SLEEP_MATRIX=${SLEEP_MATRIX:-"10 50 100 300"}
ENDPOINT_MATRIX=${ENDPOINT_MATRIX:-"blocking suspend"}
OUT_DIR=${OUT_DIR:-$REPO_DIR/load/k6/results}

if [[ ! -f "$SCRIPT_FILE" ]]; then
  echo "k6 script not found: $SCRIPT_FILE" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

echo "Running k6 matrix against $BASE_URL (duration=$DURATION rates=$RATE_MATRIX)"

# Prevent host environment K6_* values from overriding script scenarios.
unset K6_VUS K6_DURATION K6_ITERATIONS K6_STAGES K6_SCENARIOS K6_EXECUTION_SEGMENT

for rate in $RATE_MATRIX; do
  for endpoint in $ENDPOINT_MATRIX; do
    for sleep_ms in $SLEEP_MATRIX; do
      out_file="$OUT_DIR/${endpoint}-sleep${sleep_ms}-rate${rate}.txt"
      echo ""
      echo "=== rate=$rate endpoint=$endpoint sleepMs=$sleep_ms ==="
      BASE_URL="$BASE_URL" \
      ENDPOINT="$endpoint" \
      SLEEP_MS="$sleep_ms" \
      DURATION="$DURATION" \
      RATE="$rate" \
      PRE_ALLOCATED_VUS="$PRE_ALLOCATED_VUS" \
      MAX_VUS="$MAX_VUS" \
      k6 run "$SCRIPT_FILE" | tee "$out_file"
    done
  done
done

echo ""
echo "Saved outputs to $OUT_DIR"
