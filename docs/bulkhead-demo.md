# Bulkhead với Resilience4j – cô lập việc parse CV chậm trong cv-parse-service

> **Thông điệp cần chốt với người xem:**
> *"Parse CV chậm (~8s) thì chỉ việc parse CV bị chậm; các chức năng khác của cv-parse-service vẫn phục vụ bình thường."*
> Demo cả **Semaphore** và **ThreadPool** trên **cùng một service** để so sánh trực tiếp.

---

## 1. Bối cảnh

```
                       cv-parse-service :8085  (profile demo: Tomcat 20 thread)
bulkhead-load.sh ──►   ├─ /analyze-no-bulkhead/{cvId}   không giới hạn
(40 CV cùng lúc)       ├─ /analyze/{cvId}               @Bulkhead SEMAPHORE  "cvParse"     5 permit, chờ ≤ 5s
                       ├─ /analyze-threadpool/{cvId}    @Bulkhead THREADPOOL "cvParsePool" 5 thread + 6 hàng đợi
                       └─ /ping                         KHÔNG parse – thước đo "service còn sống?"
```

Cả 3 endpoint parse cùng làm 1 việc (giả lập `Thread.sleep(8000)`), chỉ khác cách bulkhead.

---

## 2. Code ở đâu

| File | Vai trò |
|---|---|
| `cv-parse-service/.../application/CvParseService.java` | `parse()` – **`@Bulkhead(name = "cvParse", type = SEMAPHORE)`**<br>`parseOnThreadPool()` – **`@Bulkhead(name = "cvParsePool", type = THREADPOOL)`**, trả `CompletableFuture`<br>`parseWithoutBulkhead()` – để so sánh |
| `cv-parse-service/.../api/CvParseController.java` | 3 endpoint parse + `/ping`; bắt `BulkheadFullException` → HTTP 503 |
| `cv-parse-service/.../resources/application.yml` | `resilience4j.bulkhead.instances.cvParse` (5 permit, chờ 5s)<br>`resilience4j.thread-pool-bulkhead.instances.cvParsePool` (core 3, max 5, queue 6) |
| `cv-parse-service/.../resources/application-demo.yml` | Profile demo: Tomcat chỉ 20 thread (để thấy cạn thread với 40 request) |
| `docs/bulkhead-load.sh` | Bắn N CV đồng thời theo 1 chế độ, đo `/ping`, in chi tiết từng CV |

---

## 3. Chuẩn bị (10 phút trước)

1. Bật PostgreSQL (DB `ats_cvs_dbs`).
2. IntelliJ → cấu hình chạy **CvParseServiceApplication**:
   * Active profiles = `demo`
   * Environment variables: `SPRING_DATASOURCE_USERNAME=postgres;SPRING_DATASOURCE_PASSWORD=<mật khẩu>`
3. Start **EurekaServer → CvParseServiceApplication**. (Không cần application-service.)
4. Git Bash – 2 terminal:
   * **A** (chạy lệnh): `cd docs`
   * **B** (xem log): `tail -f ~/cv-parse-demo.log | grep --line-buffered "\[CV-"`
5. Kiểm tra: `curl -s localhost:8085/api/v1/cvparses/ping; echo`

---

## 3b. Dashboard trực quan (thay cho log – nên dùng khi trình chiếu)

Mở trình duyệt: **http://localhost:8085/bulkhead-demo.html** (cần profile `demo`).
Bấm **Không bulkhead / Semaphore / ThreadPool** để bắn 40 CV, trang cập nhật 2 lần/giây:

| Ô trên trang | Ý nghĩa |
|---|---|
| **/ping – chức năng khác** | độ trễ thật do trình duyệt đo, số tăng dần khi service treo + biểu đồ 60s |
| **Thread Tomcat** (20 ô cam) | thread phục vụ mọi request – hết ô = service treo |
| **Semaphore** | 5 ô tím = permit đang dùng · ô đỏ = thread Tomcat đứng CHỜ permit |
| **ThreadPool** | 5 ô xanh = worker riêng · 6 ô vàng = hàng đợi (không giữ thread Tomcat) |
| **Kết quả từng CV** | xám = đang chờ · xanh = 200 · đỏ = 503, kèm số giây |
| **So sánh các lượt chạy** | mỗi lượt xong tự thêm 1 dòng: /ping chậm nhất, đỉnh Tomcat, 200/503… |

Việc bắn tải do endpoint `/actuator/bulkheaddemo` (cổng 8086) làm, vì trình duyệt chỉ mở ~6 kết nối song song.

## 4. Kịch bản trình diễn (~10 phút)

### Màn 1 – Không có bulkhead (2 phút)
```bash
./bulkhead-load.sh no-bulkhead 40
```
**Kết quả đo thực tế:**
```
ping #1 -> HTTP 200 trong 15.1s            <-- chức năng KHÔNG liên quan cũng treo
HTTP 200 :  40 request | 8s .. 16s         <-- 20 CV parse cùng lúc, 20 CV chờ thread Tomcat
```
**Nói:** "40 CV ập vào, cả 20 thread Tomcat đều bị việc parse chiếm hết → `/ping` phải chờ 15 giây.
Nếu parse là OCR/AI thật, 20 việc nặng cùng lúc còn làm service quá tải CPU/RAM."

### Màn 2 – Semaphore (3 phút)
```bash
./bulkhead-load.sh semaphore 40
```
**Kết quả đo thực tế:**
```
ping #1 -> HTTP 200 trong 9.2s             <-- vẫn treo!
HTTP 200 :  10 request | 8s .. 16s         <-- tối đa 5 CV parse cùng lúc
HTTP 503 :  30 request | 5s .. 13s         <-- chờ permit đủ 5s rồi mới bị từ chối
```
Terminal B: `(2) >>> VAO: nhan 1 permit | dang chay 5/5`, rồi `(x) XXX TU CHOI: het thoi gian cho ...`
**Mở code:** `CvParseService.parse()` + block `bulkhead.cvParse` (5 permit, 5s).
**Nói:**
* ✅ Semaphore giới hạn được **số CV parse cùng lúc** (5) → bảo vệ CPU/RAM.
* ❌ Nhưng nó chạy trên **chính thread Tomcat**: CV đang parse giữ thread 8s, CV chờ permit giữ thread tới 5s
  → thread Tomcat vẫn cạn → `/ping` vẫn treo 9 giây.

### Màn 3 – ThreadPool (3 phút)
```bash
./bulkhead-load.sh thread-pool 40
```
**Kết quả đo thực tế:**
```
ping -> 0.004s                                  <-- service vẫn sống
HTTP 200 :  11 request | 8s / 16s / 24s         <-- 5 chạy ngay + 6 xếp hàng, chạy theo từng đợt 5
HTTP 503 :  29 request | ~0.01s                 <-- bị từ chối NGAY, không giữ thread nào
CV-1  XU LY XONG ... cho trong hang doi    30 ms + xu ly 8014 ms
CV-6  XU LY XONG ... cho trong hang doi  8000 ms + xu ly 8011 ms
CV-9  XU LY XONG ... cho trong hang doi 15980 ms + xu ly 8009 ms
```
Terminal B: `(1) DEN SERVICE tren http-nio-8085-exec-N` → `(2b) BAT DAU PARSE tren bulkhead-cvParsePool-N | hang doi y/6`
→ `(3) <<< RA` → `(4) TRA VE 200`; CV bị từ chối: `(x) XXX TU CHOI NGAY: pool + hang doi day | dang chay 5/5 | hang doi 6/6`.
**Mở code:** `CvParseService.parseOnThreadPool()` (trả `CompletableFuture`) + block `thread-pool-bulkhead.cvParsePool`
+ `CvParseController.analyzeThreadPool()`.
**Nói:**
* Việc parse chạy trên **pool riêng** `bulkhead-cvParsePool-*`; thread Tomcat được **trả về ngay** → `/ping` nhanh.
* Có **hàng đợi**: CV-6 chờ ~8s, CV-9 chờ ~16s rồi mới được parse – bulkhead lấy từ hàng đợi khi có thread rảnh.
* Đầy pool + hàng đợi → 503 **ngay lập tức** thay vì bắt người dùng chờ rồi mới từ chối.
* Cái giá: chạy trên thread khác nên mất ThreadLocal (MDC, SecurityContext, header) – xem log `(2b)`.

### Màn 4 – So sánh trực quan 2 cách xử lý (2 phút)
```bash
./bulkhead-compare.sh 40 semaphore thread-pool
```
Mỗi giây vẽ 1 dòng: thanh **thread Tomcat đang bận**, số **CV đang parse**, số **CV đang chờ** – rồi in bảng so sánh.
```
semaphore   1.8s   [####################] 20/20   parse 5   cho 15   <-- 15 thread Tomcat đứng CHỜ permit
thread-pool 1.9s   [....................]  0/20   parse 5   cho 6    <-- 6 CV trong HÀNG ĐỢI, Tomcat rảnh
```
**Nói:** "Cả hai đều chỉ cho 5 CV parse cùng lúc. Khác nhau ở chỗ **chờ bằng gì**: Semaphore bắt request
đứng chờ trên thread Tomcat (cạn thread → /ping 9.6s), ThreadPool cất request vào hàng đợi (Tomcat 0/20 → /ping 0.003s)."
(Actuator chạy ở cổng riêng 8086 nên vẫn đọc được số liệu khi Tomcat chính đã cạn thread.)

### Kết thúc
Mở `http://localhost:8086/actuator/bulkheads` – thấy cả `cvParse` (semaphore) và `cvParsePool` (thread pool).

---

## 5. Bảng so sánh (slide cuối) – số liệu đo thật, 40 CV, Tomcat 20 thread

| | Không bulkhead | Semaphore `cvParse` | ThreadPool `cvParsePool` |
|---|---|---|---|
| Giới hạn | ❌ | 5 permit | 5 thread + 6 hàng đợi |
| Chạy trên | thread Tomcat | **thread Tomcat** | **pool riêng** `bulkhead-cvParsePool-*` |
| Khi đầy | – | chờ ≤ 5s rồi 503 | 503 **ngay** |
| `/ping` trong lúc tải | **15.1s** | **9.2s** | **0.004s** |
| Thành công / bị từ chối | 40 / 0 | 10 / 30 | 11 / 29 |
| Kiểu trả về | bất kỳ | bất kỳ | bắt buộc `CompletableFuture` |
| ThreadLocal (MDC, Security) | còn | còn | **mất** |
| Bảo vệ | – | tài nguyên CPU/RAM khi parse | tài nguyên **và** thread phục vụ các chức năng khác |

---

## 6. Câu hỏi hay bị hỏi

* **Vì sao Semaphore vẫn làm treo /ping?** Semaphore chỉ đếm "vé"; việc parse và việc chờ vé đều diễn ra trên thread Tomcat.
  Giảm `maxWaitDuration` về 0 thì CV bị từ chối ngay, nhưng 5 CV đang parse vẫn giữ 5 thread Tomcat.
* **Vì sao 11 CV được nhận?** `maxThreadPoolSize (5) + queueCapacity (6) = 11`.
* **Thứ tự nhận việc của ThreadPool?** core (3) → hàng đợi (6) → max (5): pool chỉ tạo thread thứ 4, 5 khi hàng đợi đã đầy.
* **Khác Rate Limiter?** Rate limiter giới hạn *số request trong một khoảng thời gian*; bulkhead giới hạn *số request đang chạy cùng lúc*.
* **Khác Circuit Breaker?** Circuit breaker ngắt mạch khi tỉ lệ lỗi/chậm cao; bulkhead chỉ chia tài nguyên. Thường dùng chung.
* **Chọn con số thế nào?** Định luật Little: số việc đồng thời ≈ throughput × latency. Ví dụ 0.5 CV/s × 8s = 4 → chọn 5.

> Ghi chú: application-service cũng có ThreadPool bulkhead `cvParse` (CvParseGateway) bảo vệ phía gọi khi nộp hồ sơ.
> Phần đó là tích hợp thật, không nằm trong kịch bản demo này.
