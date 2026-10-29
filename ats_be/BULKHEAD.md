# Bulkhead (Resilience4j) – cô lập dịch vụ parse CV chậm

> Mục tiêu: một đợt CV ồ ạt cần parse **không được làm chậm** chức năng chấm điểm CV của HR,
> và **không làm mất** CV nào.

## 1. Vấn đề

Luồng CV: `candidate-service` → MinIO → RabbitMQ → `cv-parse-service` → RabbitMQ → `candidate-service`.

Trong `cv-parse-service`, hai loại việc dùng chung **engine phân tích** (OCR/AI) có năng lực giới hạn
(mô phỏng bằng `AnalysisEngine`: 5 việc cùng lúc):

| Loại việc | Kiểu | Ai chờ |
|---|---|---|
| Parse CV hàng loạt (từ RabbitMQ) | bất đồng bộ | không ai đứng chờ |
| HR `POST /api/v1/cvparse/match`, `/analyze` | đồng bộ | HR đứng chờ trên màn hình |

Không có bulkhead: đợt CV hàng loạt chiếm hết 5 slot, yêu cầu của HR phải xếp hàng phía sau.

## 2. Giải pháp – chia ngăn engine 5 slot = 3 + 2

| Ngăn | Kiểu Resilience4j | Cấu hình (`config-repo/cv-parse-service.yml`) | Khi đầy |
|---|---|---|---|
| `cvParseBatch` – parse hàng loạt | **ThreadPool** (bất đồng bộ) | 3 thread + 17 hàng đợi, prefetch RabbitMQ 20 | Tin nhắn chờ an toàn trong RabbitMQ; tràn ngăn → requeue sau 1s |
| `cvParseInteractive` – API của HR | **Semaphore** (đồng bộ) | 2 permit, chờ tối đa 0,5s | 503 `CV_PARSE_BUSY` ngay |

Bất đồng bộ đúng nghĩa: listener RabbitMQ trả `CompletableFuture`, ack mode `MANUAL` → container
chỉ ack khi parse xong; thread của listener được giải phóng ngay, việc parse chạy trên `bulkhead-cvParseBatch-*`.

### Code chính (`services/cv-parse-service`)

| File | Vai trò |
|---|---|
| `engine/AnalysisEngine.java` | Engine năng lực giới hạn (fair semaphore), độ trễ mô phỏng cấu hình được (mặc định 0s) |
| `bulkhead/IsolatedTasks.java` | `@Bulkhead(THREADPOOL)` cho batch, `@Bulkhead(SEMAPHORE)` cho HR |
| `bulkhead/CvParseExecutor.java` | Chọn đường đi: qua bulkhead hoặc không giới hạn (khi demo tắt bulkhead) |
| `messaging/CvParseMessageListener.java` | Nhận lệnh → `submitBatch` → trả `CompletableFuture` |
| `messaging/CvParseRabbitConfiguration.java` | ack `MANUAL`, `prefetch` |
| `controller/CvParseController.java` | `/match`, `/analyze` qua ngăn Semaphore |
| `bulkhead/BulkheadMonitorController.java` | `GET /api/v1/cvparse/bulkhead`, `PUT /api/v1/cvparse/bulkhead/mode` |

## 3. Chạy demo

```bash
cp .env.example .env
mvn -DskipTests clean package
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
```

`docker-compose.demo.yml` chỉ dùng khi demo: parse 8s, chấm điểm 2s, cho phép bật/tắt bulkhead,
giao diện ở **http://localhost:3000** (đăng nhập `admin / admin123`).

| Trang | Dùng để |
|---|---|
| `tiep-nhan.html` | Nộp hồ sơ + CV, trạng thái tự cập nhật, mô phỏng đợt cao điểm |
| `hr.html` | HR chấm điểm CV theo JD, đo thời gian phản hồi |
| `giam-sat.html` | Bật/tắt bulkhead, engine, 2 ngăn, hàng đợi RabbitMQ, đo HR liên tục, bảng so sánh |

### Kịch bản (~8 phút)

1. **Giám sát** → *Tắt bulkhead* → *Mô phỏng nộp CV (40)*. Engine 5/5 slot màu xanh dương,
   HR "đang chờ kết quả…" tăng tới **~30 giây**.
2. *Bật bulkhead* → *Mô phỏng nộp CV (40)*. Engine 3 xanh dương + 1–2 xanh lá, hàng đợi ngăn 17/17,
   tin nhắn chờ trong RabbitMQ giảm dần, HR luôn **~2 giây**.
3. Bảng **So sánh các lượt** đặt 2 lượt cạnh nhau.

Kịch bản đầy đủ có lời thoại, giải thích màn hình và đi code từng phần: [docs/KICH-BAN-DEMO.md](docs/KICH-BAN-DEMO.md).

### Kết quả đo thật (40 CV cùng lúc)

| | Tắt bulkhead | Bật bulkhead |
|---|---|---|
| Nộp 40 hồ sơ | 0,57 s | 0,54 s |
| HR chấm điểm chậm nhất | **32,0 s** | **2,1 s** |
| HR trung bình | 21,5 s | 2,0 s |
| Parse hết 40 CV | 67 s | 114 s |
| CV bị mất / vào DLQ | 0 | 0 |

Đánh đổi có chủ đích: parse hàng loạt chậm hơn (chỉ dùng 3/5 slot) để HR luôn có 2 slot riêng.

## 4. Sửa lỗi phát hiện khi chạy end-to-end

| Lỗi | Hệ quả | Sửa |
|---|---|---|
| `candidate-service` gửi `CvParseCommand` bằng `convertAndSend(CV_RESULT_ROUTING_KEY, command)` (exchange mặc định, sai routing key) | RabbitMQ bỏ tin nhắn, CV kẹt mãi ở `PENDING` | Gửi `CV_EXCHANGE` + `CV_PARSE_ROUTING_KEY`, **sau khi transaction commit** |
| Không có code tạo bucket MinIO | Upload lỗi `NoSuchBucket` | `candidate-service` tự tạo bucket khi khởi động |
| `minio/minio` đã bị gỡ khỏi Docker Hub/quay.io | `docker compose up` lỗi pull | Dùng `pgsty/minio` (bản fork cộng đồng, cùng lệnh/biến môi trường) |

Sơ đồ luồng end-to-end: `docs/bulkhead-e2e.drawio`.
