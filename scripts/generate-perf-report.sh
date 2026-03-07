#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
K6_DIR=${K6_DIR:-$REPO_DIR/load/k6/results}
WRK_DIR=${WRK_DIR:-$REPO_DIR/load/wrk/results}
OUT_FILE=${OUT_FILE:-$REPO_DIR/load/reports/perf-summary.md}

mkdir -p "$(dirname "$OUT_FILE")"

extract_case_from_filename() {
  local path filename endpoint sleep_ms extra tomcat_tag case_tag
  path=$1
  filename=$(basename "$1")
  endpoint=$(printf "%s" "$filename" | sed -nE 's/^(.+)-sleep[0-9]+.*\.txt$/\1/p')
  sleep_ms=$(printf "%s" "$filename" | sed -nE 's/^.+-sleep([0-9]+).+\.txt$/\1/p')
  extra=$(printf "%s" "$filename" | sed -nE 's/^.+-sleep[0-9]+-(.+)\.txt$/\1/p')
  tomcat_tag=$(printf "%s" "$path" | grep -oE 'tomcat-max[0-9]+' | head -n 1 || true)

  if [[ -z "$endpoint" ]]; then
    endpoint=${filename%%-sleep*}
  fi
  if [[ -z "$sleep_ms" ]]; then
    sleep_ms=${filename#*-sleep}
    sleep_ms=${sleep_ms%.txt}
  fi
  if [[ -z "$extra" || "$extra" == "$filename" ]]; then
    extra="-"
  fi

  if [[ -n "$tomcat_tag" ]]; then
    if [[ "$extra" == "-" ]]; then
      case_tag="$tomcat_tag"
    else
      case_tag="$tomcat_tag/$extra"
    fi
  else
    case_tag="$extra"
  fi

  printf "%s|%s|%s" "$endpoint" "$sleep_ms" "$case_tag"
}

parse_k6_file() {
  local file endpoint sleep_ms case_tag p95 p99 rps case_info duration_line req_line
  file=$1
  case_info=$(extract_case_from_filename "$file")
  endpoint=${case_info%%|*}
  sleep_ms=$(printf "%s" "$case_info" | awk -F'|' '{print $2}')
  case_tag=$(printf "%s" "$case_info" | awk -F'|' '{print $3}')

  duration_line=$(grep -E "http_req_duration" "$file" | head -n 1 || true)
  req_line=$(grep -E "http_reqs" "$file" | head -n 1 || true)

  p95=$(printf "%s" "$duration_line" | sed -nE 's/.*p\(95\)=([^ ]+).*/\1/p')
  p99=$(printf "%s" "$duration_line" | sed -nE 's/.*p\(99\)=([^ ]+).*/\1/p')
  rps=$(printf "%s" "$req_line" | grep -oE '[0-9]+(\.[0-9]+)?/s' | head -n 1 || true)

  p95=${p95:-N/A}
  p99=${p99:-N/A}
  rps=${rps:-N/A}

  printf "| k6 | %s | %s | %s | %s | %s | %s |\n" "$endpoint" "$sleep_ms" "$case_tag" "$p95" "$p99" "$rps"
}

parse_wrk_file() {
  local file endpoint sleep_ms case_tag p95 p99 rps case_info
  file=$1
  case_info=$(extract_case_from_filename "$file")
  endpoint=${case_info%%|*}
  sleep_ms=$(printf "%s" "$case_info" | awk -F'|' '{print $2}')
  case_tag=$(printf "%s" "$case_info" | awk -F'|' '{print $3}')

  p95=$(grep -E "^[[:space:]]*95%" "$file" | awk '{print $2}' | tail -n 1 || true)
  p99=$(grep -E "^[[:space:]]*99%" "$file" | awk '{print $2}' | tail -n 1 || true)
  rps=$(grep -E "^Requests/sec:" "$file" | awk '{print $2}' | tail -n 1 || true)

  p95=${p95:-N/A}
  p99=${p99:-N/A}
  rps=${rps:-N/A}

  printf "| wrk | %s | %s | %s | %s | %s | %s |\n" "$endpoint" "$sleep_ms" "$case_tag" "$p95" "$p99" "$rps"
}

{
  echo "# Performance Summary"
  echo ""
  echo "- Generated at: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
  echo "- k6 results dir: \`$K6_DIR\`"
  echo "- wrk results dir: \`$WRK_DIR\`"
  echo ""
  echo "| Tool | Endpoint | sleepMs | Case | p95 | p99 | RPS |"
  echo "| --- | --- | ---: | --- | ---: | ---: | ---: |"

  if [[ -d "$K6_DIR" ]] && find "$K6_DIR" -type f -name "*.txt" | grep -q .; then
    while IFS= read -r f; do
      parse_k6_file "$f"
    done < <(find "$K6_DIR" -type f -name "*.txt" | sort)
  fi

  if [[ -d "$WRK_DIR" ]] && find "$WRK_DIR" -type f -name "*.txt" | grep -q .; then
    while IFS= read -r f; do
      parse_wrk_file "$f"
    done < <(find "$WRK_DIR" -type f -name "*.txt" | sort)
  fi

  echo ""
  echo "## Notes"
  echo ""
  echo '- wrk 기본 `--latency` 출력은 p50/p75/p90/p99는 제공하지만 p95는 제공하지 않습니다.'
  echo '- 따라서 별도 퍼센타일 확장 구성이 없으면 wrk 행의 `p95`는 `N/A`로 표시됩니다.'
} > "$OUT_FILE"

echo "Wrote report: $OUT_FILE"
