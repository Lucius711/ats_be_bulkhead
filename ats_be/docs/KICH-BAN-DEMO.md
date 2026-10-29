# Kịch bản demo – Bulkhead (Resilience4j) cô lập dịch vụ parse CV chậm

> Thời lượng: khoảng 20 phút (demo giao diện ~10 phút, đi code ~8 phút, kết luận ~2 phút).
> Trong kịch bản: **🎤** là lời thoại, **👉** là thao tác, **👀** là điều khán giả nhìn thấy trên màn hình.
> Chỉ nói về bulkhead; các phần khác (Keycloak, Gateway, MinIO…) chỉ là hạ tầng, không cần giải thích.

---

## 0. Chuẩn bị trước khi lên trình bày (làm trước 5 phút)

1. Bật Docker Desktop, chạy stack ở chế độ demo (engine chậm + cho phép bật/tắt bulkhead):
   ```bash
   cd ats_be
   docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d
   ```
   Chờ khoảng 1–2 phút cho các service lên hết.
2. Mở trình duyệt, **chỉ một tab**: http://localhost:3000 → đăng nhập `admin` / `admin123` → vào **Giám sát bulkhead**.
   - Đóng mọi tab localhost:3000 khác (tab khác cũng chấm điểm và chiếm "vé" của HR → kết quả sai).
   - Nhấn **Ctrl+Shift+R** một lần để chắc chắn đang dùng giao diện mới nhất.
3. Trên trang Giám sát:
   - Bấm **Xoá bảng** (bảng so sánh) và **Xoá lịch sử** (lịch sử test HR).
   - Công tắc **HR chấm liên tục**: **bật**.
   - Ô "Đợt cao điểm": **40**. Ô "Số HR bấm cùng lúc": **10**.
   - Bấm **Tắt bulkhead** (bắt đầu từ trạng thái "chưa có giải pháp").
4. Mở IntelliJ với project **`C:\MSS\FA26_MSS301_SE1947-JV\ats_be`** (nhánh `feature/bulkhead-rabbitmq`), mở sẵn các file ở mục 6.
5. Mở sẵn sơ đồ `docs/bulkhead-e2e.drawio` (nếu muốn chiếu luồng tổng thể).

---

## 1. Mở đầu – đặt vấn đề (1 phút)

🎤 *"Hệ thống ATS của nhóm có service **cv-parse-service** làm hai việc:*
- *Thứ nhất, **parse CV hàng loạt**: ứng viên nộp CV, candidate-service gửi tin nhắn vào RabbitMQ, cv-parse lấy ra phân tích. Không ai đứng chờ việc này, chậm vài phút cũng được.*
- *Thứ hai, **HR chấm điểm CV theo mô tả công việc**: HR bấm nút và đứng chờ kết quả trên màn hình, nên phải nhanh.*

*Cả hai việc đều dùng chung một **máy phân tích CV**, mô phỏng một dịch vụ OCR/AI chậm, **chỉ phục vụ được 5 việc cùng lúc**. Parse 1 CV mất 8 giây, chấm điểm 1 CV mất 2 giây.*

*Câu hỏi đặt ra: khi có **đợt nộp CV ồ ạt**, ví dụ 40 CV cùng lúc, thì HR có còn chấm điểm được không? Đây chính là bài toán mà **Bulkhead** giải quyết."*

🎤 *"Bulkhead là vách ngăn trong thân tàu: một khoang bị thủng nước thì nước không tràn sang khoang khác. Trong phần mềm, ta **chia tài nguyên thành các ngăn riêng** cho từng loại việc, để một loại việc quá tải không kéo sập loại việc khác."*

---

## 2. Giới thiệu màn hình Giám sát (2 phút)

👉 Chỉ chuột lần lượt từ trên xuống.

**(a) Thanh trên cùng**
🎤 *"Đây là trạng thái bulkhead: hiện đang **TẮT**. Hai nút này bật/tắt bulkhead ngay lúc chạy, không cần khởi động lại service, chỉ để demo so sánh. Bên phải là nút **Mô phỏng nộp CV**: giả lập 40 ứng viên nộp CV cùng lúc."*

**(b) Câu giải thích màu (xanh/vàng/đỏ)**
🎤 *"Dòng này mô tả bằng lời chuyện gì đang xảy ra, cập nhật mỗi giây."*

**(c) Sơ đồ luồng: đọc từ trái sang phải**
🎤 *"Sơ đồ chia làm 3 cột:*
1. ***Việc đến**: hàng trên là CV cần parse (ô cam là CV còn chờ trong RabbitMQ); hàng dưới là HR bấm chấm điểm. Công tắc **HR chấm liên tục** giả lập một HR cứ khoảng 1,5 giây lại bấm chấm điểm một lần.*
2. ***Đi qua ngăn (bulkhead)**: hàng trên là ngăn của parse, hàng dưới là ngăn của HR. Khi tắt bulkhead, hai ô này đỏ và ghi 'Không có ngăn'.*
3. ***Máy phân tích CV – 5 chỗ**: 5 ô vuông là 5 chỗ của máy. Ô xanh dương là đang parse, ô xanh lá là đang chấm điểm cho HR. Bên dưới là **hàng chờ trước máy**: những việc đã bắt đầu nhưng chưa có chỗ, chờ theo thứ tự ai đến trước vào trước."*

🎤 *"Lưu ý: **mỗi ô có màu là một thread đang ở trong máy**. Máy 5 chỗ là thứ khan hiếm thật sự, gọi là nút cổ chai, và bulkhead sẽ chia 5 chỗ này."*

**(d) Test nhiều HR cùng lúc + lịch sử**
🎤 *"Khung này cho N HR bấm chấm điểm **cùng một lúc** và ghi lại kết quả từng người: được chấm (200) hay bị báo bận (503), mất bao lâu."*

**(e) Biểu đồ + bảng so sánh**
🎤 *"Biểu đồ là thời gian phản hồi của HR theo thời gian: cột xanh nhanh, cam chậm, đỏ rất chậm. Nền hồng là lúc tắt bulkhead, nền xanh là lúc bật. Bảng cuối trang ghi lại mỗi lượt mô phỏng để so sánh trước và sau."*

---

## 3. Demo 1 – KHÔNG có bulkhead: HR bị kẹt (3 phút)

👉 Kiểm tra: **ĐANG TẮT**, công tắc **HR chấm liên tục** bật. Bấm **Mô phỏng nộp CV** (40).

👀 Trong vài giây:
- Ô "Không có ngăn" của parse hiện **khoảng 20 thread đang parse** (mỗi CV một thread, không giới hạn).
- Máy phân tích: **cả 5 ô xanh dương "Parse"**.
- Hàng chờ trước máy: khoảng 15 ô xanh dương, rồi **1 ô đỏ** là yêu cầu của HR.
- Câu giải thích chuyển **đỏ**: *"Bulkhead TẮT – HR đang bị kẹt! … HR phải chờ khoảng 26 s hoặc hơn."*

🎤 *"Không có ngăn, mỗi CV được một thread riêng, thread nhiều bao nhiêu cũng được. Nhưng **máy chỉ có 5 chỗ**, nên 5 việc parse chiếm hết, khoảng 15 việc parse khác đứng chờ. Và yêu cầu của HR, ô đỏ, phải **đứng sau tất cả** các việc đó. Máy phục vụ theo thứ tự đến, nên HR phải chờ khoảng 15 việc ÷ 5 chỗ × 8 giây, tức khoảng 25 giây."*

👉 Khoảng 10–15 giây sau khi bấm mô phỏng, bấm **Nhiều HR chấm điểm cùng lúc** (10 HR).

👀 Nút quay một lúc lâu. Kết quả: **cả 10 HR được chấm (200) nhưng mất khoảng 25–60 giây**. Biểu đồ có cột đỏ cao.

🎤 *"10 HR cùng bấm lúc đang có đợt CV. Không ai bị từ chối, nhưng **ai cũng phải đợi nửa phút đến một phút** cho một thao tác lẽ ra chỉ mất 2 giây. Với người dùng, đây coi như hệ thống bị treo. Một phần chậm, là parse hàng loạt, đã **kéo sập** phần cần nhanh, là HR. Đây gọi là lỗi dây chuyền (cascading failure)."*

👉 Chờ lượt mô phỏng xong (khoảng 70 giây, dòng *"Xong lượt bulkhead TẮT…"*). Trong lúc chờ, có thể nói trước phần giải pháp ở mục 4.

👀 Bảng so sánh có dòng **Tắt**: HR chậm nhất khoảng **30–60 s**, parse hết 40 CV sau khoảng **67 s**.

---

## 4. Demo 2 – BẬT bulkhead: HR luôn nhanh (3 phút)

🎤 *"Giải pháp: chia 5 chỗ của máy thành **2 ngăn** bằng Resilience4j:*
- ***Ngăn parse, loại ThreadPool**: chỉ có **3 thread riêng** và 17 ghế chờ. Parse không bao giờ chiếm quá 3 chỗ của máy.*
- ***Ngăn HR, loại Semaphore**: **2 vé**. Tối đa 2 HR được vào máy cùng lúc; người thứ 3 chờ tối đa 0,5 giây, hết vé thì được báo bận ngay (503), không bị treo.*

*3 + 2 = 5, đúng bằng số chỗ của máy, nên **luôn còn 2 chỗ dành riêng cho HR**."*

👉 Bấm **Bật bulkhead**. 👀 Ô "giữ cho HR" viền nét đứt xuất hiện ở 2 chỗ cuối của máy; hai ngăn hết đỏ.

👉 Bấm **Mô phỏng nộp CV** (40).

👀
- Ngăn parse: **3/3 thread** xanh dương, **ghế chờ 17/17** màu vàng, phần còn lại (ô cam) **chờ trong RabbitMQ**.
- Máy phân tích: **3 ô Parse**, 2 ô cuối là **HR** hoặc **"giữ cho HR"**. Hàng chờ trước máy: **"không có ai"**.
- Câu giải thích **xanh**: *"Bulkhead BẬT. Parse đang dùng 3/3 chỗ (còn 17 CV chờ trong ngăn, … CV chờ trong RabbitMQ). 2 chỗ còn lại luôn giữ cho HR…"*
- Biểu đồ HR: cột xanh **khoảng 2 s** đều đặn.

🎤 *"Cùng 40 CV, nhưng giờ parse chỉ được dùng 3 chỗ. CV thừa không bị mất, chúng **chờ trong hàng đợi của ngăn và trong RabbitMQ**. Hai chỗ cuối luôn để dành cho HR, nên HR vẫn chấm điểm khoảng 2 giây, **như thể không có đợt CV nào**."*

👉 Bấm **Nhiều HR chấm điểm cùng lúc** (10 HR).

👀 Nút xong sau khoảng 2 giây. **2 HR được chấm (khoảng 2,0 s)**, **8 HR bị từ chối 503 sau khoảng 0,5 s**.

🎤 *"10 HR bấm cùng lúc: đúng 2 người có vé được chấm ngay. 8 người còn lại chờ đúng 0,5 giây, không có vé thì nhận **thông báo 'hệ thống bận, thử lại sau vài giây'**. Không ai phải chờ nửa phút. Nhận câu trả lời nhanh, kể cả là 'thử lại', vẫn tốt hơn nhiều so với bị treo."*

👉 Chờ lượt mô phỏng xong (khoảng 115 giây). 👀 Bảng so sánh có dòng **Bật**.

🎤 (chỉ vào bảng so sánh)

| | HR chậm nhất | Parse hết 40 CV |
|---|---|---|
| Tắt | **khoảng 30–60 s** | khoảng 67 s |
| Bật | **khoảng 2,1 s** | khoảng 114 s |

*"Bulkhead **không làm máy nhanh hơn**. Parse hàng loạt còn chậm hơn: 114 giây so với 67 giây, vì chỉ được dùng 3 chỗ thay vì 5. Đây là cái giá phải trả có chủ đích: **hi sinh một chút thông lượng của việc không gấp để đảm bảo độ trễ của việc gấp**. CV chậm 1 phút không ai để ý, còn HR chờ 1 phút là trải nghiệm hỏng."*

---

## 5. Demo 3 – Riêng ngăn Semaphore, khi hệ thống rảnh (1–2 phút)

👉 Khi không có lượt mô phỏng nào đang chạy. Đặt **Số HR = 15**.

👉 **Bật** bulkhead → bấm **Nhiều HR chấm điểm cùng lúc**.
👀 **2 × 200 (khoảng 2 s)**, **13 × 503 (khoảng 0,5 s)**.

👉 **Tắt** bulkhead → bấm lại.
👀 Cả 15 được chấm nhưng **chia 3 đợt: 5 HR sau 2 s, 5 HR sau 4 s, 5 HR sau 6 s**.

🎤 *"Khi tắt, 15 HR tranh nhau 5 chỗ của máy nên đi thành 3 đợt: đây chính là giới hạn 5 chỗ hiện ra rất rõ. Khi bật, ngăn Semaphore chỉ cho 2 HR vào.*

*Nói thật lòng: lúc hệ thống rảnh, bật bulkhead trông còn 'tệ hơn' vì nhiều người bị 503 trong khi máy còn chỗ trống. Đó là **đánh đổi của bulkhead tĩnh**: giới hạn cứng cả lúc không cần. Đổi lại, ta có một **cam kết**: dù hệ thống đang tải nặng hay nhẹ, HR luôn nhận câu trả lời trong khoảng 2 giây."*

👉 Bấm **Bật bulkhead** lại trước khi sang phần code.

> Tuỳ chọn (30 giây): mở trang **HR chấm điểm CV**, chọn 1 CV, bấm **Chấm điểm** để khán giả thấy giao diện thật của HR: điểm phù hợp, kỹ năng khớp/thiếu. Khi bị 503, trang hiện *"Hệ thống chấm điểm đang bận – thử lại sau vài giây"*.

---

## 6. Đi vào code (8 phút)

🎤 *"Giờ xem code làm việc đó như thế nào. Toàn bộ nằm trong `services/cv-parse-service`, đi theo đúng thứ tự trên sơ đồ."*

### 6.1. Thư viện – [pom.xml:70-80](../services/cv-parse-service/pom.xml#L70-L80)
```xml
<artifactId>resilience4j-spring-boot4</artifactId>   <!-- 2.4.0 -->
<artifactId>spring-boot-starter-aspectj</artifactId>
```
🎤 *"Hai dependency: **Resilience4j** cho Spring Boot 4, và **AspectJ** vì annotation `@Bulkhead` hoạt động bằng **AOP**. Spring bọc bean bằng một proxy; khi gọi method có annotation, proxy chặn lại, xin chỗ trong bulkhead rồi mới cho chạy."*

### 6.2. Cấu hình hai ngăn – [config-repo/cv-parse-service.yml:26-40](../config-repo/cv-parse-service.yml#L26-L40)
```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      cvParseBatch:
        core-thread-pool-size: 3
        max-thread-pool-size: 3
        queue-capacity: 17
        keep-alive-duration: 20ms
  bulkhead:
    instances:
      cvParseInteractive:
        max-concurrent-calls: 2
        max-wait-duration: 500ms
```
🎤 *"Toàn bộ thông số bulkhead nằm ở cấu hình, không nằm trong code:*
- ***`cvParseBatch`**, loại ThreadPool: `core` = `max` = **3 thread** cố định, nên parse chiếm tối đa 3 chỗ máy. `queue-capacity: 17` là 17 ghế chờ trong ngăn. Con số 17 không chọn ngẫu nhiên: **17 = prefetch 20 − 3 thread**, nên số tin nhắn RabbitMQ giao cho service không bao giờ vượt sức chứa ngăn (3 đang chạy + 17 chờ = 20). `keep-alive` chỉ áp dụng cho thread vượt core; ở đây core = max nên gần như không có tác dụng.*
- ***`cvParseInteractive`**, loại Semaphore: `max-concurrent-calls: 2` là **2 vé**; `max-wait-duration: 500ms` là thời gian chờ vé tối đa, quá thì ném `BulkheadFullException`.*

*Đặt cạnh nhau: [dòng 14](../config-repo/cv-parse-service.yml#L14) `engine.capacity: 5` = **3 + 2**. Bulkhead được tính để chia đúng tài nguyên thật."*

🎤 *"Config này nằm trong **Config Server**, nên muốn đổi 3/2 thành 4/1 thì chỉ sửa YAML, không phải sửa code."*

### 6.3. Tài nguyên được bảo vệ – [engine/AnalysisEngine.java](../services/cv-parse-service/src/main/java/fu/ats/engine/AnalysisEngine.java)
```java
this.slots = new Semaphore(capacity, true);           // dòng 30: 5 chỗ, công bằng (FIFO)

public <T> T run(Workload workload, Duration cost, Supplier<T> work) {
    slots.acquire();                                  // dòng 41: hết chỗ thì đứng chờ
    ...
    Thread.sleep(cost.toMillis());                    // dòng 52: giả lập việc nặng (8 s / 2 s)
    return work.get();
    ...
    slots.release();                                  // dòng 60: trả chỗ
}
```
🎤 *"Đây **không phải Resilience4j**, đây là thứ mà bulkhead bảo vệ: một máy phân tích giả lập chỉ có 5 chỗ. Dùng `java.util.concurrent.Semaphore` với `fair = true` nên ai đến trước vào trước. Chính vì công bằng mà khi không có ngăn, HR phải xếp sau toàn bộ việc parse đến trước. Ngoài thực tế, đây có thể là API AI giới hạn số request đồng thời, số GPU, hay một connection pool."*

### 6.4. Hai ngăn bulkhead – [bulkhead/IsolatedTasks.java](../services/cv-parse-service/src/main/java/fu/ats/bulkhead/IsolatedTasks.java)
```java
@Bulkhead(name = BulkheadNames.BATCH, type = Bulkhead.Type.THREADPOOL)        // dòng 20
public CompletableFuture<Void> runBatch(Runnable task) {
    task.run();
    return CompletableFuture.completedFuture(null);
}

@Bulkhead(name = BulkheadNames.INTERACTIVE, type = Bulkhead.Type.SEMAPHORE)   // dòng 30
public <T> T runInteractive(Supplier<T> task) {
    return task.get();
}
```
🎤 *"Đây là trái tim của demo. Hai method gần như rỗng: chỉ chạy việc được giao. Toàn bộ 'phép màu' nằm ở annotation:*

- ***`type = THREADPOOL`**: khi `runBatch` được gọi, proxy **không chạy method trên thread của người gọi**. Nó **đẩy lời gọi vào pool riêng** của ngăn (`bulkhead-cvParseBatch-1..3`) và trả ngay một `CompletableFuture` cho người gọi. Vì vậy method **bắt buộc trả `CompletableFuture`**. Nếu 3 thread đều bận thì việc vào hàng đợi 17 chỗ; hàng đợi cũng đầy thì ném `BulkheadFullException`. Đây là **cách ly bằng thread**: việc parse chậm không bao giờ chạy trên thread của listener RabbitMQ hay Tomcat.*
- ***`type = SEMAPHORE`**: method **chạy ngay trên thread của người gọi**, tức thread Tomcat của request HR; không tạo thread mới. Proxy chỉ xin 1 trong 2 vé trước khi chạy, chạy xong trả vé. Chờ quá 500 ms không có vé thì ném `BulkheadFullException`. Đây là **cách ly bằng giới hạn số lượng**: rẻ, đơn giản, hợp với API đồng bộ vì đằng nào HR cũng đang đứng chờ trên thread đó.*

*Tên `cvParseBatch` / `cvParseInteractive` khớp với tên trong YAML; đó là cách annotation lấy đúng cấu hình. Các tên này gom trong [BulkheadNames.java](../services/cv-parse-service/src/main/java/fu/ats/bulkhead/BulkheadNames.java)."*

🎤 *"Vì sao phải tách hai method này ra **một bean riêng**? Vì `@Bulkhead` chạy qua **proxy**. Nếu một class tự gọi `this.runBatch()` thì lời gọi đi thẳng vào object, **không qua proxy**, annotation bị bỏ qua. Đây là lỗi kinh điển khi dùng AOP."*

**Vì sao chọn ThreadPool cho parse, Semaphore cho HR?**

| | Parse CV hàng loạt → **ThreadPool** | HR chấm điểm → **Semaphore** |
|---|---|---|
| Ai chờ | Không ai, chạy nền | HR đứng chờ trên màn hình |
| Cần gì | Trả thread listener về ngay, có hàng đợi đệm | Trả lời nhanh, đầy thì từ chối ngay |
| Thread | Pool riêng 3 thread | Dùng luôn thread Tomcat của request |
| Khi đầy | Xếp hàng (17), tràn thì trả về RabbitMQ | Chờ 0,5 s rồi 503 |

### 6.5. Công tắc bật/tắt – [bulkhead/CvParseExecutor.java:39-53](../services/cv-parse-service/src/main/java/fu/ats/bulkhead/CvParseExecutor.java#L39-L53)
```java
public CompletableFuture<Void> submitBatch(Runnable task) {
    if (!mode.enabled()) {
        return CompletableFuture.runAsync(task, unbounded);   // TẮT: pool không giới hạn
    }
    try {
        return isolatedTasks.runBatch(task);                  // BẬT: qua ngăn ThreadPool
    } catch (BulkheadFullException exception) {
        return CompletableFuture.failedFuture(exception);
    }
}

public <T> T interactive(Supplier<T> task) {
    return mode.enabled() ? isolatedTasks.runInteractive(task) : task.get();
}
```
🎤 *"Lớp này là **điểm vào duy nhất**, để phần còn lại của code không phải biết đang bật hay tắt. Khi **tắt**, parse chạy trên một `newCachedThreadPool` không giới hạn ([dòng 31-35](../services/cv-parse-service/src/main/java/fu/ats/bulkhead/CvParseExecutor.java#L31-L35), thread tên `parse-unbounded-N`), đúng như những gì ta thấy ở Demo 1: 20 thread lao vào máy 5 chỗ.*

*Công tắc [BulkheadMode](../services/cv-parse-service/src/main/java/fu/ats/bulkhead/BulkheadMode.java) chỉ đổi được khi `toggle-enabled=true`. Biến này chỉ bật trong [docker-compose.demo.yml](../docker-compose.demo.yml); chạy thật thì bulkhead luôn bật."*

### 6.6. Parse hàng loạt đi vào ngăn – [messaging/CvParseMessageListener.java:45-56](../services/cv-parse-service/src/main/java/fu/ats/messaging/CvParseMessageListener.java#L45-L56)
```java
@RabbitListener(queues = RabbitMessagingNames.CV_PARSE_QUEUE)
public CompletableFuture<Void> handle(CvParseCommand command) {
    return cvParseExecutor.submitBatch(() -> process(command))
            .exceptionallyCompose(error -> requeueLaterIfBusy(command, error));
}
```
🎤 *"Listener RabbitMQ **không tự parse**. Nó giao việc cho ngăn ThreadPool rồi **trả về `CompletableFuture` ngay**. Thread listener rảnh tay nhận tin nhắn tiếp theo. Đây là phần **bất đồng bộ** của ThreadPool bulkhead.*

*Bên trong `process()`, [dòng 79-83](../services/cv-parse-service/src/main/java/fu/ats/messaging/CvParseMessageListener.java#L79-L83), việc parse gọi `engine.run(Workload.BATCH, parseDelay, …)`: xin 1 chỗ của máy, chiếm 8 giây.*

*Nếu ngăn đầy, [`requeueLaterIfBusy`](../services/cv-parse-service/src/main/java/fu/ats/messaging/CvParseMessageListener.java#L62-L71) đợi 1 giây rồi ném `ImmediateRequeueAmqpException`: tin nhắn **quay lại RabbitMQ** để xử lý sau, **không vào dead-letter, không mất CV**."*

### 6.7. Ack khi parse xong + prefetch – [messaging/CvParseRabbitConfiguration.java:129-132](../services/cv-parse-service/src/main/java/fu/ats/messaging/CvParseRabbitConfiguration.java#L129-L132)
```java
factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
factory.setPrefetchCount(properties.batch().prefetch());   // 20
```
🎤 *"Hai dòng này để ThreadPool bulkhead chạy an toàn với RabbitMQ:*
- ***`MANUAL`**: vì listener trả `CompletableFuture`, Spring chỉ **ack khi future hoàn thành**, tức là khi parse xong. Service có chết giữa chừng thì tin nhắn chưa ack vẫn còn trong RabbitMQ.*
- ***`prefetch 20`**: RabbitMQ chỉ giao tối đa 20 tin nhắn chưa ack cho service; phần còn lại nằm chờ trong RabbitMQ. Đây là **backpressure**: những ô cam 'chờ trong RabbitMQ' trên giao diện. Kết hợp với ngăn 3 + 17 = 20, ngăn không bao giờ tràn."*

### 6.8. API của HR đi vào ngăn – [controller/CvParseController.java:45-55](../services/cv-parse-service/src/main/java/fu/ats/controller/CvParseController.java#L45-L55)
```java
@PostMapping("/match")
public CvMatchResponse match(@Valid @RequestBody CvMatchRequest request) {
    return cvParseExecutor.interactive(() -> engine.run(
            Workload.INTERACTIVE,
            properties.engine().matchDelay(),     // 2 s
            () -> cvParseService.match(request)
    ));
}
```
🎤 *"API chấm điểm bọc toàn bộ việc dùng máy trong `interactive(...)`: phải **có vé Semaphore** trước rồi mới được xin chỗ của máy. Vì tối đa 2 vé và máy luôn còn 2 chỗ không bị parse chiếm, HR có vé là có chỗ ngay. API `/analyze` ở trên ([dòng 33-43](../services/cv-parse-service/src/main/java/fu/ats/controller/CvParseController.java#L33-L43)) cũng đi qua cùng ngăn."*

### 6.9. Đầy ngăn → 503 – [bulkhead/BulkheadExceptionHandler.java:16-24](../services/cv-parse-service/src/main/java/fu/ats/bulkhead/BulkheadExceptionHandler.java#L16-L24)
```java
@ExceptionHandler(BulkheadFullException.class)
public ResponseEntity<Map<String, Object>> handleBulkheadFull(BulkheadFullException exception) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("code", "CV_PARSE_BUSY",
                         "message", "CV analysis is busy, please retry in a few seconds", ...));
}
```
🎤 *"Khi Semaphore hết vé quá 0,5 giây, `BulkheadFullException` được đổi thành **HTTP 503** với mã `CV_PARSE_BUSY`. Đây là cái 8 HR nhận được ở Demo 2, và giao diện dựa vào nó để hiện 'thử lại sau vài giây'. **Fail fast**: từ chối nhanh và rõ ràng thay vì để client treo rồi timeout."*

### 6.10. Số liệu trên giao diện lấy từ đâu – [bulkhead/BulkheadMonitorController.java:48-73](../services/cv-parse-service/src/main/java/fu/ats/bulkhead/BulkheadMonitorController.java#L48-L73)
```java
ThreadPoolBulkhead.Metrics batch = threadPoolBulkheadRegistry.bulkhead(BulkheadNames.BATCH).getMetrics();
Bulkhead interactive = bulkheadRegistry.bulkhead(BulkheadNames.INTERACTIVE);
...
"activeThreads", batch.getActiveThreadCount(),        // 3 thread của ngăn parse
"queueDepth",    batch.getQueueDepth(),               // ghế chờ x/17
"availableConcurrentCalls", interactive.getMetrics().getAvailableConcurrentCalls()   // vé còn lại
```
🎤 *"Resilience4j quản lý mọi bulkhead trong hai **registry**. Trang Giám sát gọi API này mỗi giây: số thread đang chạy và độ sâu hàng đợi lấy thẳng từ **metrics của Resilience4j**; số vé còn lại cũng vậy. Trên giao diện không có số nào bị 'vẽ cho đẹp', tất cả là trạng thái thật của bulkhead. Ngoài ra Actuator cũng mở `/actuator/bulkheads` ([yml dòng 46](../config-repo/cv-parse-service.yml#L46)) để giám sát chuẩn."*

---

## 7. Kết luận (1–2 phút)

🎤 *"Tóm lại:*
1. *Vấn đề: một tài nguyên chậm, máy phân tích 5 chỗ, bị **dùng chung** giữa việc không gấp (parse hàng loạt) và việc gấp (HR). Không có ngăn, đợt CV ồ ạt làm **HR chờ 30–60 giây**.*
2. *Giải pháp: **Bulkhead** của Resilience4j chia tài nguyên thành 2 ngăn:*
   - ***ThreadPool** cho parse: 3 thread riêng + hàng đợi, **bất đồng bộ**, kết hợp RabbitMQ (ack khi xong, prefetch) để không mất CV.*
   - ***Semaphore** cho HR: 2 vé, **đồng bộ**, đầy thì **503 sau 0,5 giây**.*
3. *Kết quả đo được: HR chậm nhất từ **khoảng 30–60 s xuống khoảng 2 s**, 0 CV bị mất. Cái giá là parse hàng loạt chậm hơn (67 s → 114 s): một đánh đổi có chủ đích.*
4. *Bulkhead **không làm hệ thống nhanh hơn**. Nó **cô lập lỗi**: một phần quá tải không kéo sập phần khác."*

---

## 8. Câu hỏi hay gặp – chuẩn bị sẵn câu trả lời

**Hỏi: Sao không tăng số thread lên cho nhanh?**
Đáp: Nút cổ chai là **máy 5 chỗ**, không phải thread. Demo 1 đã có khoảng 20 thread mà HR vẫn kẹt, vì thread thừa chỉ đứng chờ trước máy và tốn bộ nhớ. Thêm thread không thêm chỗ cho máy.

**Hỏi: Thread và "chỗ của máy" khác gì nhau?**
Đáp: Ví dụ dễ hiểu: **cây cầu 5 làn** (máy) và **tài xế** (thread). Xe tải là parse, xe cứu thương là HR. Không có bulkhead, xe tải chiếm cả 5 làn và xe cứu thương kẹt phía sau. Ngăn ThreadPool là đội xe tải chỉ có 3 tài xế; ngăn Semaphore là 2 vé vào làn ưu tiên.

**Hỏi: ThreadPool và Semaphore bulkhead khác nhau thế nào?**
Đáp: ThreadPool **tạo thread riêng** và có hàng đợi, chạy bất đồng bộ (method phải trả `CompletableFuture`). Semaphore **không tạo thread**, chỉ đếm số lời gọi đồng thời, chạy luôn trên thread của người gọi. Xem bảng ở mục 6.4.

**Hỏi: HR bị 503 thì sao, có phải là lỗi không?**
Đáp: Đó là **từ chối có chủ đích** (fail fast). Trả lời trong 0,5 giây kèm "thử lại sau" tốt hơn treo 60 giây. Client có thể tự thử lại sau vài giây; ngoài thực tế có thể kết hợp Retry hoặc hiển thị hàng đợi.

**Hỏi: Ngăn parse đầy thì CV có bị mất không?**
Đáp: Không. Tin nhắn chỉ được ack khi parse xong (`MANUAL`). Ngăn đầy thì tin nhắn được **requeue** về RabbitMQ; prefetch 20 cũng giữ phần thừa trong RabbitMQ. Đo thực tế: 0 CV vào dead-letter queue.

**Hỏi: Chọn con số 3 / 2 / 17 thế nào?**
Đáp: Tổng 3 + 2 = sức chứa máy (5). Chia theo ưu tiên: HR ít nhưng cần nhanh nên giữ 2; parse đông nhưng không gấp nên lấy 3. Hàng đợi 17 = prefetch 20 − 3. Ngoài thực tế dùng **định luật Little**: số việc đồng thời = thông lượng × thời gian xử lý, rồi đo và tinh chỉnh.

**Hỏi: Lúc hệ thống rảnh mà HR vẫn bị 503, có lãng phí không?**
Đáp: Có, đó là nhược điểm của bulkhead **tĩnh** (Demo 3). Có thể cải tiến bằng giới hạn động (adaptive concurrency) hoặc cho parse "mượn" chỗ của HR khi rảnh, nhưng phức tạp hơn và mất cam kết độ trễ cho HR.

**Hỏi: Một thread xử lý được nhiều request cùng lúc không?**
Đáp: Trong mô hình chặn (Spring MVC) thì không: một thread làm một việc mỗi lúc, xong mới nhận việc khác. Với reactive (WebFlux, như API Gateway) hoặc virtual thread thì một thread thật phục vụ được nhiều request. Nhưng máy 5 chỗ vẫn chỉ có 5 chỗ, nên vẫn cần Semaphore bulkhead.

---

## 9. Xử lý sự cố khi đang demo

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Trang không có sơ đồ luồng / nút mới | Trình duyệt dùng bản cũ trong cache | **Ctrl+Shift+R** |
| Bật bulkhead mà chỉ 1 HR (hoặc 0) được chấm | Tab khác đang cầm vé (trang HR "Tự chấm điểm", tab Giám sát khác) | Đóng tab khác. Kết quả test sẽ ghi *"Lúc bấm đã có x/2 vé đang bị HR khác cầm"* |
| Ô vé HR nhảy 0/2 ↔ 1/2 liên tục | Công tắc **HR chấm liên tục** đang bật | Bình thường; tắt công tắc nếu muốn đứng yên |
| Nút bật/tắt bulkhead bị mờ | Đang có lượt mô phỏng chạy | Chờ dòng "Xong lượt…" |
| `ERR_CONNECTION_REFUSED` ở localhost:3000 | Docker Desktop chưa chạy | Mở Docker Desktop, chạy lại lệnh ở mục 0 |
| Muốn thử hơn 15 HR | Trình duyệt giới hạn số kết nối | `./scripts/test-semaphore.sh 30` |
