package fu.ats.client.cvparse;

import fu.ats.exception.ServiceBusyException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Cửa DUY NHẤT để gọi sang cv-parse-service, có gắn ThreadPool Bulkhead.
 *
 * Vì sao ThreadPool?
 *  - Parse CV chậm (~8s) và không cần kết quả ngay khi nộp hồ sơ -> chạy nền trên pool riêng
 *    "bulkhead-cvParse-*", thread Tomcat của application-service không bao giờ phải chờ parse.
 *  - Pool + hàng đợi có giới hạn: cv-parse chậm cỡ nào thì application-service cũng chỉ mất tối đa
 *    max-thread-pool-size thread cho nó; phần vượt quá bị từ chối ngay thay vì dồn ứ.
 *
 * Vì sao tách lớp riêng? @Bulkhead chạy qua Spring AOP proxy, gọi nội bộ this.xxx() sẽ bỏ qua annotation.
 */
@Component
@RequiredArgsConstructor
public class CvParseGateway {

    public static final String BULKHEAD = "cvParse";

    private final CvParseClient cvParseClient;

    @Bulkhead(name = BULKHEAD, type = Bulkhead.Type.THREADPOOL, fallbackMethod = "cvParseBusy")
    public CompletableFuture<String> parse(String cvId) {
        // Thân method đã chạy trên thread của bulkhead, gọi Feign đồng bộ rồi bọc kết quả lại là đủ
        return CompletableFuture.completedFuture(cvParseClient.analyze(cvId));
    }

    /** Fallback: cùng tham số với method gốc + 1 tham số Throwable ở cuối. */
    private CompletableFuture<String> cvParseBusy(String cvId, Throwable error) {
        return CompletableFuture.failedFuture(ServiceBusyException.fromBulkhead("cv-parse-service", error));
    }
}
