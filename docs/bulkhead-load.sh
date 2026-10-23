#!/usr/bin/env bash
# =====================================================================================
# Bắn N request parse CV ĐỒNG THỜI vào cv-parse-service, trong lúc đó đo endpoint /ping "vô can".
# Cả 3 chế độ đều nằm trong CÙNG cv-parse-service (:8085), parse ~8s / CV.
# Chạy bằng Git Bash (Windows) hoặc terminal macOS/Linux.
#
#   ./bulkhead-load.sh no-bulkhead      # không giới hạn gì
#   ./bulkhead-load.sh semaphore        # Semaphore bulkhead "cvParse"      (5 permit, chờ tối đa 5s)
#   ./bulkhead-load.sh thread-pool      # ThreadPool bulkhead "cvParsePool" (5 thread + 6 hàng đợi)
#   ./bulkhead-load.sh thread-pool 60   # đổi số request đồng thời (mặc định 40)
# =====================================================================================
set -u
export LC_ALL=C   # dấu thập phân là "." (Windows tiếng Việt mặc định dùng ",")

MODE=${1:?"Cach dung: $0 <no-bulkhead|semaphore|thread-pool> [so_request]"}
N=${2:-40}
SVC=${CVPARSE_URL:-http://localhost:8085}/api/v1/cvparses
case "$MODE" in
  no-bulkhead) EP=analyze-no-bulkhead ;;
  semaphore)   EP=analyze ;;
  thread-pool) EP=analyze-threadpool ;;
  *) echo "MODE phai la no-bulkhead | semaphore | thread-pool"; exit 1 ;;
esac
OUT=$(mktemp -d)

echo
echo "=============================================================="
echo " MODE = $MODE   |   $N CV parse dong thoi   |   GET /$EP/{cvId}"
echo "=============================================================="

T0=$(date +%s)
for i in $(seq 1 "$N"); do
  curl -s --max-time 120 -o "$OUT/body-$i.txt" -w "%{http_code} %{time_total}\n" \
       "$SVC/$EP/CV-$i" > "$OUT/res-$i.txt" &
done

# Đợi tải dồn vào rồi mới đo endpoint không liên quan
sleep 0.5
echo
echo "--- /ping cua cv-parse-service (KHONG parse CV) trong luc dang tai:"
for k in 1 2 3; do
  curl -s --max-time 120 -o /dev/null -w "    ping #$k -> HTTP %{http_code} trong %{time_total}s\n" "$SVC/ping"
  sleep 0.3
done

wait
T1=$(date +%s)

echo
echo "--- Ket qua $N request (tong thoi gian ~$((T1 - T0))s):"
cat "$OUT"/res-*.txt | awk '
  { n[$1]++; s[$1]+=$2; if ($2 > mx[$1]) mx[$1]=$2; if (!($1 in mn) || $2 < mn[$1]) mn[$1]=$2 }
  END { for (c in n) printf "    HTTP %s : %3d request | min %.2fs | avg %.2fs | max %.2fs\n", c, n[c], mn[c], s[c]/n[c], mx[c] }' | sort

echo
echo "--- Chi tiet CV DUOC XU LY (sap xep theo thoi gian phan hoi):"
for i in $(seq 1 "$N"); do
  read -r code t < "$OUT/res-$i.txt"
  [ "$code" = "200" ] || continue
  awk -v t="$t" -v b="$(cat "$OUT/body-$i.txt")" 'BEGIN { printf "%s    %6.2fs  %s\n", t, t, b }'
done | sort -g | cut -d' ' -f2-

# Các request không thành công, gom theo mã HTTP, kèm 1 response mẫu
for code in $(cut -d' ' -f1 "$OUT"/res-*.txt | grep -v '^200$' | sort -u); do
  ids=$(grep -l "^$code " "$OUT"/res-*.txt | sed 's/.*res-\([0-9]*\)\.txt/\1/' | sort -n)
  echo
  echo "--- HTTP $code ($(echo $ids | wc -w) request):"
  echo "$ids" | sed 's/^/CV-/' | tr '\n' ' ' | fold -s -w 100 | sed 's/^/    /'; echo
  echo "    Mau: $(head -c 300 "$OUT/body-$(echo "$ids" | head -1).txt")"
done

rm -rf "$OUT"
