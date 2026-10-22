#!/usr/bin/env bash
# =====================================================================================
# Bắn N request ĐỒNG THỜI "parse CV" vào application-service, trong lúc đó đo endpoint /ping "vô can".
# Mỗi request parse 1 CV riêng (CV-1, CV-2, ...) qua cv-parse-service (chậm ~8s).
# Chạy bằng Git Bash (Windows) hoặc terminal macOS/Linux.
#
#   ./bulkhead-load.sh no-bulkhead      # TRƯỚC khi có bulkhead
#   ./bulkhead-load.sh thread-pool      # SAU: ThreadPool bulkhead
#   ./bulkhead-load.sh thread-pool 60   # đổi số request đồng thời (mặc định 40)
# =====================================================================================
set -u
export LC_ALL=C   # dấu thập phân là "." (Windows tiếng Việt mặc định dùng ",")

MODE=${1:?"Cach dung: $0 <no-bulkhead|thread-pool> [so_request]"}
N=${2:-40}
APP=${APP_URL:-http://localhost:8083}
OUT=$(mktemp -d)

echo
echo "=============================================================="
echo " MODE = $MODE   |   $N request parse CV dong thoi"
echo "=============================================================="

T0=$(date +%s)
for i in $(seq 1 "$N"); do
  curl -s --max-time 120 -o "$OUT/body-$i.json" \
       -w "%{http_code} %{time_total}\n" \
       -H "X-Correlation-Id: req-$i" \
       "$APP/api/v1/applications/demo/cv-parse/CV-$i/$MODE" > "$OUT/res-$i.txt" &
done

# Đợi tải dồn vào rồi mới đo endpoint không liên quan
sleep 0.5
echo
echo "--- /ping (endpoint KHONG goi cv-parse-service) trong luc dang tai:"
for k in 1 2 3; do
  curl -s --max-time 120 -o /dev/null \
       -w "    ping #$k -> HTTP %{http_code} trong %{time_total}s\n" \
       "$APP/api/v1/applications/demo/ping"
  sleep 0.3
done

wait
T1=$(date +%s)

echo
echo "--- Ket qua $N request (tong thoi gian ~$((T1 - T0))s):"
cat "$OUT"/res-*.txt | awk '
  { n[$1]++; s[$1]+=$2; if ($2 > mx[$1]) mx[$1]=$2; if (!($1 in mn) || $2 < mn[$1]) mn[$1]=$2 }
  END { for (c in n) printf "    HTTP %s : %3d request | min %.2fs | avg %.2fs | max %.2fs\n", c, n[c], mn[c], s[c]/n[c], mx[c] }' | sort

ok=$(grep -l '"executedOn"' "$OUT"/body-*.json 2>/dev/null | head -1)
echo
if [ -n "$ok" ]; then echo "--- Mau response thanh cong:"; cat "$ok"; echo; fi

# Lấy 1 field từ JSON response (không cần jq)
field() { sed -n "s/.*\"$2\":\"\{0,1\}\([^\",}]*\).*/\1/p" "$1"; }

echo
echo "--- Chi tiet tung request DUOC XU LY (sap xep theo thoi gian phan hoi):"
printf "    %-8s %-6s %-9s %-11s %-11s %s\n" "REQUEST" "HTTP" "TONG" "CHO-QUEUE" "PARSE-CV" "THREAD XU LY"
for i in $(seq 1 "$N"); do
  read -r code t < "$OUT/res-$i.txt"
  [ "$code" = "200" ] || continue
  b="$OUT/body-$i.json"
  # awk thay vì printf %f: printf của Git Bash không đọc được số thập phân trên Windows tiếng Việt
  awk -v t="$t" -v id="req-$i" -v c="$code" -v q="$(field "$b" queueMs)" -v p="$(field "$b" callMs)" \
      -v th="$(field "$b" executedOn)" 'BEGIN { printf "%s    %-8s %-6s %7.2fs %8sms %8sms   %s\n", t, id, c, t, q, p, th }'
done | sort -g | cut -d' ' -f2-

# Các request không thành công, gom theo mã HTTP, kèm 1 response mẫu
for code in $(cut -d' ' -f1 "$OUT"/res-*.txt | grep -v '^200$' | sort -u); do
  ids=$(grep -l "^$code " "$OUT"/res-*.txt | sed 's/.*res-\([0-9]*\)\.txt/\1/' | sort -n)
  case "$code" in
    503) why="application-service TU CHOI ngay (ThreadPool bulkhead day)" ;;
    502) why="cv-parse-service tu choi (Semaphore phia cv-parse day) - thread Tomcat da bi giu trong luc cho" ;;
    *)   why="loi khac" ;;
  esac
  echo
  echo "--- HTTP $code ($(echo $ids | wc -w) request): $why"
  echo "$ids" | sed 's/^/req-/' | tr '\n' ' ' | fold -s -w 100 | sed 's/^/    /'; echo
  echo "    Mau: $(head -c 600 "$OUT/body-$(echo "$ids" | head -1).json")"
done

rm -rf "$OUT"
