#!/usr/bin/env bash
# =====================================================================================
# Kiểm tra ngăn Semaphore "cvParseInteractive" (2 permit, chờ tối đa 0,5s) của cv-parse-service.
# Gửi N yêu cầu HR chấm điểm CV (POST /api/v1/cvparse/match) CÙNG LÚC qua API Gateway.
#
#   ./scripts/test-semaphore.sh          # 6 yêu cầu, bulkhead bật rồi tắt để so sánh
#   ./scripts/test-semaphore.sh 10       # đổi số yêu cầu
#
# Kỳ vọng khi BẬT (demo: chấm điểm mất ~2s): 2 x 200 (~2s), phần còn lại 503 sau ~0,5s.
# Cần chạy stack với docker-compose.demo.yml (cho phép bật/tắt bulkhead).
# =====================================================================================
set -u
export LC_ALL=C
N=${1:-6}
GW=${GATEWAY_URL:-http://localhost:8080}
OUT=$(mktemp -d)
BODY='{"cvText":"Backend Developer 4 years. Skills: Java, Spring Boot, PostgreSQL, Docker","jobDescription":"Java, Spring Boot, Docker, Kubernetes"}'

TOKEN=$(curl -s -X POST http://localhost:8180/realms/ats/protocol/openid-connect/token \
  -d grant_type=password -d client_id=ats-web -d username=admin -d password=admin123 \
  | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || { echo "Khong lay duoc token tu Keycloak"; exit 1; }

set_mode() {
  curl -s -o /dev/null -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d "{\"enabled\":$1}" "$GW/api/v1/cvparse/bulkhead/mode"
}
match() {   # $1 = file ghi kết quả
  curl -s -o "$1.body" -w "%{http_code} %{time_total}\n" -H "Authorization: Bearer $TOKEN" \
       -H "Content-Type: application/json" -d "$BODY" "$GW/api/v1/cvparse/match" > "$1"
}

match "$OUT/warmup"   # làm nóng

for MODE in true false; do
  set_mode "$MODE"
  echo
  echo "== Bulkhead $([ "$MODE" = true ] && echo BAT || echo TAT) | $N HR cham diem CUNG LUC"
  for i in $(seq 1 "$N"); do match "$OUT/$MODE-$i" & done
  wait
  for i in $(seq 1 "$N"); do read -r code t < "$OUT/$MODE-$i"; echo "$t HR-$i $code"; done | sort -g |
    awk '{ printf "   %-5s -> HTTP %s sau %.2fs\n", $2, $3, $1 }'
  busy=$(grep -l '"CV_PARSE_BUSY"' "$OUT"/$MODE-*.body 2>/dev/null | head -1)
  [ -n "$busy" ] && echo "   503 tra ve: code=CV_PARSE_BUSY, $(grep -o '"message":"[^"]*"' "$busy" | head -1)"
  sleep 3
done

set_mode true
echo
echo "(da bat lai bulkhead)"
rm -rf "$OUT"
