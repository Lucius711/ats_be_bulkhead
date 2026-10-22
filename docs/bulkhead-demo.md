# Bulkhead với Resilience4j – cô lập dịch vụ parse CV chậm của ATS

> **Thông điệp cần chốt với người xem:**
> *"Parse CV chậm thì chỉ việc parse CV bị chậm theo; nộp hồ sơ và mọi chức năng khác của application-service vẫn sống."*

---

## 1. Bối cảnh

```
Ứng viên nộp hồ sơ
   │
   ▼
application-service :8083 ──[ThreadPool Bulkhead "cvParse"]──Feign──► cv-parse-service :8085 (parse ~8s)
   │   • tối đa 5 thread parse + 6 chỗ hàng đợi, còn lại bị từ chối ngay           └─[Semaphore Bulkhead "cvParse"]
   │   • thread Tomcat KHÔNG phải chờ parse                                              tối đa 5 CV parse cùng lúc,
   └── /demo/ping (không gọi ai cả)                                                      chờ permit tối đa 5s
```

* **Vấn đề:** parse CV mất ~8s. Nếu application-service gọi parse trên thread Tomcat, mỗi lời gọi giữ 1 thread 8s.
  Hết thread Tomcat thì **mọi** endpoint của application-service đều treo, kể cả endpoint không liên quan.
* **Giải pháp – 2 lớp bulkhead:**
  1. **Phía gọi (application-service) – ThreadPool:** lời gọi parse chạy trên pool riêng `bulkhead-cvParse-*`, có giới hạn.
     Đây là lớp **cô lập**: cv-parse chậm cỡ nào cũng chỉ chiếm tối đa 5 thread của application-service.
  2. **Phía bị gọi (cv-parse-service) – Semaphore:** tự giới hạn tối đa 5 CV parse cùng lúc để không tự quá tải.
     Lớp này bảo vệ cv-parse, **nhưng không bảo vệ được người gọi** (màn 1 sẽ chứng minh).

---

## 2. Code ở đâu

| File | Vai trò |
|---|---|
| `application-service/.../client/cvparse/CvParseClient.java` | Feign client gọi `GET /api/v1/cvparses/analyze/{cvId}` |
| `application-service/.../client/cvparse/CvParseGateway.java` | **`@Bulkhead(name = "cvParse", type = THREADPOOL)`** + fallback → 503 |
| `application-service/.../service/CvParseDispatcher.java` | Nộp hồ sơ → **sau khi commit** mới gửi CV đi parse nền: xong → `PARSED`, bulkhead đầy/cv-parse bận → giữ `PENDING`, lỗi khác → `FAILED` |
| `application-service/.../service/impl/ApplicationServiceImpl.java` | `createApplication` lưu `cvParseStatus = PENDING` rồi gọi dispatcher (nộp hồ sơ không chờ parse) |
| `application-service/.../entity/CvParseStatus.java` + `Application.cvParseStatus` | Trạng thái parse CV của hồ sơ (cột `cv_parse_status`) |
| `application-service/.../resources/application.yml` | `resilience4j.thread-pool-bulkhead.instances.cvParse`: core 3, max 5, queue 6 |
| `application-service/.../demo/*` | Endpoint so sánh `no-bulkhead` / `thread-pool` + `/ping`, log vòng đời từng request |
| `cv-parse-service/.../application/CvParseService.java` | **`@Bulkhead(name = "cvParse", type = SEMAPHORE)`** trên `parse()` (giả lập 8s) |
| `cv-parse-service/.../resources/application.yml` | `resilience4j.bulkhead.instances.cvParse`: maxConcurrentCalls 5, maxWaitDuration 5s |
| `cv-parse-service/demo/BulkheadLoadTest.java` | Demo riêng cho Semaphore phía cv-parse (đi qua gateway) |
| `docs/bulkhead-load.sh` | Bắn N request parse CV đồng thời vào application-service, đo `/ping` |

---

## 3. Chuẩn bị (10 phút trước)

1. Bật PostgreSQL. Cần các DB `ats_application_dbs`, `ats_cvs_dbs` (bảng tự tạo nhờ `ddl-auto: update`).
2. Cấu hình chạy trong IntelliJ, thêm Environment variables cho **application-service** và **cv-parse-service**:
   `SPRING_DATASOURCE_USERNAME=postgres;SPRING_DATASOURCE_PASSWORD=<mật khẩu>`
3. application-service chạy với **profile `demo`** (Active profiles = `demo`): Tomcat chỉ 20 thread, log ghi thêm ra
   `~/ats-demo-logs/application-service.log`.
4. Start: **eureka-server → cv-parse-service → application-service**. Đợi ~30s cho Eureka đồng bộ.
5. Làm nóng (lần gọi Feign đầu luôn chậm):
   ```bash
   curl -s localhost:8083/api/v1/applications/demo/cv-parse/CV-warm/thread-pool; echo
   ```
   Thấy `"executedOn":"bulkhead-cvParse-1"` là sẵn sàng.

---

## 4. Kịch bản trình diễn (~10 phút)

Chạy trong Git Bash: `cd docs`

### Màn 1 – TRƯỚC: không có bulkhead phía gọi (2 phút)
```bash
./bulkhead-load.sh no-bulkhead 40
```
**Kết quả đo thực tế:**
```
ping #1 -> HTTP 200 trong 9.3s          <-- endpoint KHÔNG liên quan cũng treo
HTTP 200 : 10 request | 8s .. 16s
HTTP 502 : 30 request | 5s .. 13s       <-- cv-parse (Semaphore) từ chối sau khi bắt chờ 5s
```
**Nói:**
* 20 thread Tomcat đều bị giữ chờ cv-parse → `/ping` treo 9 giây. Đây là *cascading failure*.
* cv-parse **có** Semaphore bulkhead mà application-service **vẫn chết**: Semaphore phía cv-parse chỉ bảo vệ cv-parse.
  Người gọi vẫn phải ngồi chờ (5s chờ permit rồi mới bị 502). → Cần bulkhead **phía gọi**.

### Màn 2 – Show code (3 phút)
1. `application.yml` của application-service: block `thread-pool-bulkhead.instances.cvParse` (3 / 5 / 6).
2. `CvParseGateway`: 1 annotation `@Bulkhead(THREADPOOL)` + fallback; method trả `CompletableFuture`.
3. `ApplicationServiceImpl.createApplication` + `CvParseDispatcher`: nộp hồ sơ lưu `PENDING`, parse chạy nền sau commit.
   Bulkhead đầy thì hồ sơ **vẫn nộp thành công**, CV giữ `PENDING` để parse lại sau → đúng tinh thần cô lập.
4. `CvParseService` (cv-parse-service): `@Bulkhead(SEMAPHORE)` – lớp bảo vệ thứ 2 ở phía bị gọi.

### Màn 3 – SAU: ThreadPool bulkhead phía gọi (3 phút)
```bash
./bulkhead-load.sh thread-pool 40
```
**Kết quả đo thực tế:**
```
ping -> 0.005s                                <-- service vẫn sống
HTTP 200 : 11 request | 8s / 16s / 24s        <-- 5 chạy ngay + 6 xếp hàng (chạy theo từng đợt 5)
HTTP 503 : 29 request | ~0.01s                <-- bị từ chối NGAY, không giữ thread nào
"requestThread":"http-nio-8083-exec-16", "executedOn":"bulkhead-cvParse-1"
"correlationId":"<khong co request context>"  <-- mất ThreadLocal khi đổi thread
```
Bảng "Chi tiet tung request" cho thấy cột **CHO-QUEUE**: 0s (đợt 1), ~8s (đợt 2), ~16s (đợt 3).
**Chỉ vào log** (`~/ats-demo-logs/application-service.log` hoặc console):
`[req-1] RECEIVED → START → DONE`, `[req-12] REJECTED`, `[THREADPOOL] ACCEPTED | threads x/5 | queue y/6`.

**Nói:**
* Chỉ tối đa 5 thread `bulkhead-cvParse-*` bị cv-parse giữ; 20 thread Tomcat rảnh → `/ping` nhanh.
* cv-parse chỉ nhận tối đa 5 lời gọi cùng lúc → Semaphore phía cv-parse không còn phải từ chối ai.
* Cái giá: chạy trên thread khác nên mất ThreadLocal (header, MDC, SecurityContext) – phải truyền tường minh
  (demo truyền `requestId` qua tham số `RequestInfo`).

### Màn 4 (tuỳ chọn) – Semaphore phía cv-parse-service
Cần thêm **api-gateway** (:8080). Chạy:
```bash
cd services/cv-parse-service
java demo/BulkheadLoadTest.java 15              # CÓ Semaphore: 5 chạy, phần còn lại chờ permit tối đa 5s rồi 503
java demo/BulkheadLoadTest.java 15 no-bulkhead  # KHÔNG bulkhead: cả 15 CV parse cùng lúc
```

### Kết thúc
Mở `http://localhost:8083/actuator/bulkheads` để cho thấy giám sát được bulkhead.

---

## 5. Bảng so sánh

| | Không bulkhead phía gọi | ThreadPool (application-service) | Semaphore (cv-parse-service) |
|---|---|---|---|
| Đặt ở đâu | – | **Phía gọi** | **Phía bị gọi** |
| Bảo vệ ai | – | application-service (người gọi) | cv-parse-service (chính nó) |
| Giới hạn | ❌ | 5 thread + 6 hàng đợi | 5 permit, chờ tối đa 5s |
| Thread Tomcat của người gọi khi parse chậm | bị giữ hết | **không bị giữ** | vẫn bị giữ (người gọi phải chờ) |
| Khi đầy | treo | 503 **ngay** | chờ ≤5s rồi 503 |
| Kiểu trả về | bất kỳ | bắt buộc `CompletableFuture` | bất kỳ |
| ThreadLocal / MDC / header | còn | **mất** | còn |

---

## 6. Câu hỏi hay bị hỏi

* **Vì sao parse CV hợp với ThreadPool?** Parse chậm và không cần kết quả ngay lúc nộp hồ sơ → chạy nền được.
  ThreadPool trả thread Tomcat về ngay và có hàng đợi hấp thụ tải đột biến.
* **Bulkhead đầy lúc nộp hồ sơ thì sao?** Hồ sơ vẫn lưu thành công, `cvParseStatus = PENDING` để parse lại sau.
  Lỗi của 1 phụ thuộc không làm hỏng chức năng chính – đó là "cô lập".
* **Vì sao 11 request được nhận?** `max-thread-pool-size (5) + queue-capacity (6) = 11`. Đổi `queue-capacity` là đổi con số này.
* **Khác Rate Limiter?** Rate limiter giới hạn *số request trong một khoảng thời gian*; bulkhead giới hạn *số request đang chạy cùng lúc*.
* **Khác Circuit Breaker?** Circuit breaker ngắt mạch khi tỉ lệ lỗi/chậm cao; bulkhead chỉ chia tài nguyên. Thường dùng chung.
* **Chọn con số thế nào?** Định luật Little: số lời gọi đồng thời ≈ throughput × latency. Ví dụ 0.5 CV/s × 8s = 4 → chọn 5.
* **Chạy 2 instance application-service?** Giới hạn tính theo từng instance → cv-parse có thể nhận 2 × 5 lời gọi.
  Semaphore phía cv-parse chính là lớp chặn cuối cho trường hợp này.
