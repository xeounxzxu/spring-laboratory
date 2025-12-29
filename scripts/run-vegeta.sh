#!/usr/bin/env bash
set -euo pipefail

if ! command -v vegeta >/dev/null 2>&1; then
  echo "Vegeta CLI not found. Install from https://github.com/tsenart/vegeta/releases or 'brew install vegeta'." >&2
  exit 1
fi

REPO_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
TARGETS_FILE=${TARGETS_FILE:-$REPO_DIR/load/vegeta/targets.txt}
RATE=${RATE:-20}
DURATION=${DURATION:-30s}
OUTPUT_FILE=${OUTPUT_FILE:-$REPO_DIR/load/vegeta/results.bin}
REPORT_FILE=${REPORT_FILE:-$REPO_DIR/load/vegeta/report.txt}
PLOT_FILE=${PLOT_FILE:-$REPO_DIR/load/vegeta/report.html}

if [[ ! -f "$TARGETS_FILE" ]]; then
  echo "Targets file not found: $TARGETS_FILE" >&2
  exit 1
fi

mkdir -p "$REPO_DIR/load/vegeta"

echo "Running vegeta attack (rate=$RATE, duration=$DURATION) using $TARGETS_FILE"
vegeta attack -rate "$RATE" -duration "$DURATION" -targets "$TARGETS_FILE" \
  | tee "$OUTPUT_FILE" >/dev/null

echo "Writing text report to $REPORT_FILE"
vegeta report "$OUTPUT_FILE" > "$REPORT_FILE"

if command -v vegeta >/dev/null 2>&1; then
  echo "Writing HTML plot to $PLOT_FILE"
  vegeta plot "$OUTPUT_FILE" > "$PLOT_FILE"
fi

echo "Done. Text summary:\n"
cat "$REPORT_FILE"

echo "\nView HTML latency plot via: open $PLOT_FILE"
