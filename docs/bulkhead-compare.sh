#!/usr/bin/env bash
# =====================================================================================
# So sánh TRỰC QUAN Semaphore vs ThreadPool (và không bulkhead) trên CÙNG cv-parse-service.
# Trong lúc bắn tải, mỗi giây đọc metric từ actuator (cổng riêng 8086 – không bị ảnh hưởng khi
# Tomcat chính cạn thread) và vẽ 1 dòng:
#   - Thread Tomcat đang bận: thread phục vụ MỌI chức năng của service (kể cả /ping)
#   - Đang parse:  số CV đang được parse
#   - Đang chờ:    Semaphore = thread Tomcat đứng chờ permit (VẪN GIỮ thread)
#                  ThreadPool = CV nằm trong hàng đợi (KHÔNG giữ thread Tomcat)
# Cuối cùng in bảng so sánh các chế độ cạnh nhau.
#
# Cần chạy cv-parse-service với profile demo.
#   ./bulkhead-compare.sh                                     # semaphore vs thread-pool, 40 CV
#   ./bulkhead-compare.sh 40 no-bulkhead semaphore thread-pool
# =====================================================================================
set -u
export LC_ALL=C   # dấu thập phân là "." (Windows tiếng Việt mặc định dùng ",")

N=${1:-40}
shift || true
MODES=${*:-semaphore thread-pool}
SVC=${CVPARSE_URL:-http://localhost:8085}/api/v1/cvparses
MGMT=${CVPARSE_MGMT_URL:-http://localhost:8086}/actuator/metrics

# Đọc 1 metric (số nguyên) từ actuator; $2 = tag dạng name:cvParse
metric() { curl -s --max-time 2 "$MGMT/$1${2:+?tag=$2}" | sed -n 's/.*"value":\([0-9.]*\).*/\1/p' | awk '{ printf "%d", $1 }'; }

if [ -z "$(metric tomcat.threads.config.max)" ]; then
  echo "Khong doc duoc $MGMT – hay chay cv-parse-service voi profile demo (actuator o cong 8086)."
  exit 1
fi
TOMCAT_MAX=$(metric tomcat.threads.config.max)
SEMA_MAX=$(metric resilience4j.bulkhead.max.allowed.concurrent.calls name:cvParse)
POOL_MAX=$(metric resilience4j.bulkhead.max.thread.pool.size name:cvParsePool)
QUEUE_MAX=$(metric resilience4j.bulkhead.queue.capacity name:cvParsePool)

sample() {   # in ra: tomcatBan semaDangDung poolDangChay hangDoi
  echo "$(metric tomcat.threads.busy) \
$(( SEMA_MAX - $(metric resilience4j.bulkhead.available.concurrent.calls name:cvParse) )) \
$(metric resilience4j.bulkhead.active.thread.count name:cvParsePool) \
$(metric resilience4j.bulkhead.queue.depth name:cvParsePool)"
}

wait_idle() {   # đợi lượt trước parse xong hẳn rồi mới chạy lượt mới
  for _ in $(seq 1 40); do
    [ "$(sample)" = "0 0 0 0" ] && return
    sleep 1
  done
}

declare -A R_PING R_TOMCAT R_PARSE R_WAIT R_OK R_REJ R_REJ_T

for MODE in $MODES; do
  case "$MODE" in
    no-bulkhead) EP=analyze-no-bulkhead; WAITING="cho thread Tomcat" ;;
    semaphore)   EP=analyze;             WAITING="cho permit (GIU thread Tomcat)" ;;
    thread-pool) EP=analyze-threadpool;  WAITING="hang doi (KHONG giu thread Tomcat)" ;;
    *) echo "Bo qua mode khong hop le: $MODE"; continue ;;
  esac
  wait_idle
  OUT=$(mktemp -d)

  echo
  echo "=================================================================================="
  echo " MODE = $MODE   |   $N CV dong thoi   |   GET /$EP/{cvId}"
  echo "=================================================================================="
  printf "   t     Thread Tomcat dang ban (toi da %d)    Dang parse   Dang cho: %s\n" "$TOMCAT_MAX" "$WAITING"

  T0=$(date +%s%N)
  ( for i in $(seq 1 "$N"); do
      curl -s --max-time 120 -o /dev/null -w "%{http_code} %{time_total}\n" "$SVC/$EP/CV-$i" > "$OUT/res-$i.txt" &
    done; wait ) &
  LOAD=$!
  ( sleep 0.5; curl -s --max-time 120 -o /dev/null -w "%{time_total}" "$SVC/ping" > "$OUT/ping.txt" ) &

  PEAK_T=0; PEAK_P=0; PEAK_W=0
  while kill -0 "$LOAD" 2>/dev/null; do
    read -r busy sema pool queue <<< "$(sample)"
    case "$MODE" in
      no-bulkhead) parse=$busy; waiting=0 ;;
      semaphore)   parse=$sema; waiting=$(( busy > sema ? busy - sema : 0 )) ;;
      thread-pool) parse=$pool; waiting=$queue ;;
    esac
    (( busy > PEAK_T )) && PEAK_T=$busy
    (( parse > PEAK_P )) && PEAK_P=$parse
    (( waiting > PEAK_W )) && PEAK_W=$waiting
    elapsed=$(( ($(date +%s%N) - T0) / 100000000 ))
    bar=$(awk -v b="$busy" -v m="$TOMCAT_MAX" 'BEGIN { for (i = 0; i < m; i++) printf (i < b ? "#" : "."); }')
    printf "  %2d.%ds   [%s] %2d/%-2d          %2d            %2d\n" \
           $((elapsed / 10)) $((elapsed % 10)) "$bar" "$busy" "$TOMCAT_MAX" "$parse" "$waiting"
    sleep 1
  done
  wait

  R_PING[$MODE]=$(awk '{ printf "%.3f", $1 }' "$OUT/ping.txt")
  R_TOMCAT[$MODE]="$PEAK_T/$TOMCAT_MAX"
  R_PARSE[$MODE]=$PEAK_P
  R_WAIT[$MODE]=$PEAK_W
  R_OK[$MODE]=$(grep -c "^200 " "$OUT"/res-*.txt | awk -F: '{ s += $2 } END { print s }')
  R_REJ[$MODE]=$(grep -c "^503 " "$OUT"/res-*.txt | awk -F: '{ s += $2 } END { print s }')
  R_REJ_T[$MODE]=$(grep -h "^503 " "$OUT"/res-*.txt | awk 'NR == 1 || $2 < mn { mn = $2 } $2 > mx { mx = $2 }
                     END { if (NR) printf "%.2f-%.2fs", mn, mx; else print "-" }')
  echo "  -> /ping trong luc tai: ${R_PING[$MODE]}s | 200: ${R_OK[$MODE]} | 503: ${R_REJ[$MODE]} (sau ${R_REJ_T[$MODE]})"
  rm -rf "$OUT"
done

echo
echo "=================================== SO SANH ======================================"
row() { printf "  %-38s" "$1"; shift; for v in "$@"; do printf "%-22s" "$v"; done; echo; }
row ""                                       $MODES
row "/ping (chuc nang khac) trong luc tai"   $(for m in $MODES; do echo "${R_PING[$m]:--}s"; done)
row "Thread Tomcat ban (dinh)"               $(for m in $MODES; do echo "${R_TOMCAT[$m]:--}"; done)
row "CV dang parse cung luc (dinh)"          $(for m in $MODES; do echo "${R_PARSE[$m]:--}"; done)
row "CV dang cho (dinh)"                     $(for m in $MODES; do echo "${R_WAIT[$m]:--}"; done)
row "Thanh cong (200)"                       $(for m in $MODES; do echo "${R_OK[$m]:--}"; done)
row "Bi tu choi (503)"                       $(for m in $MODES; do echo "${R_REJ[$m]:--}"; done)
row "Bi tu choi sau"                         $(for m in $MODES; do echo "${R_REJ_T[$m]:--}"; done)
echo
echo "  Semaphore : gioi han $SEMA_MAX CV parse cung luc, nhung parse + cho permit deu tren thread Tomcat."
echo "  ThreadPool: $POOL_MAX thread rieng + hang doi $QUEUE_MAX; thread Tomcat duoc tra ve ngay, day thi tu choi ngay."
