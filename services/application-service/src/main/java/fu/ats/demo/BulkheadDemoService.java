package fu.ats.demo;

import fu.ats.client.cvparse.CvParseClient;
import fu.ats.client.cvparse.CvParseGateway;
import fu.ats.exception.ServiceBusyException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.CompletableFuture;

/**
 * Cùng 1 lời gọi parse CV (CvParseClient.analyze, chậm ~8s) nhưng chạy theo 2 cách để so sánh:
 *   1. withoutBulkhead : gọi thẳng trên thread Tomcat, không giới hạn gì   -> TRƯỚC khi có bulkhead
 *   2. withThreadPool  : @Bulkhead(type = THREADPOOL)                      -> thread pool + hàng đợi riêng
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkheadDemoService {

    /** ThreadPool bulkhead dùng CHUNG instance với luồng nộp hồ sơ thật (CvParseGateway). */
    public static final String THREAD_POOL_BULKHEAD = CvParseGateway.BULKHEAD;

    private final CvParseClient cvParseClient;

    // ------------------------------------------------------------------ 1. KHÔNG bulkhead
    public CallTrace withoutBulkhead(String cvId, RequestInfo request) {
        return callCvParse("NONE", cvId, request);
    }

    // ------------------------------------------------------------------ 2. THREAD POOL
    // THREADPOOL bắt buộc method trả về CompletableFuture/CompletionStage.
    // Thân method chạy trên thread của bulkhead, nên gọi Feign đồng bộ rồi bọc kết quả lại là đủ.
    @Bulkhead(name = THREAD_POOL_BULKHEAD, type = Bulkhead.Type.THREADPOOL,
            fallbackMethod = "threadPoolFull")
    public CompletableFuture<CallTrace> withThreadPool(String cvId, RequestInfo request) {
        return CompletableFuture.completedFuture(callCvParse("THREAD_POOL", cvId, request));
    }

    private CompletableFuture<CallTrace> threadPoolFull(String cvId, RequestInfo request, Throwable error) {
        RuntimeException mapped = ServiceBusyException.fromBulkhead("cv-parse-service", error);
        if (mapped instanceof ServiceBusyException) {
            log.warn("[{}] REJECTED  | {} | pool + hang doi day -> 503", request.id(), request.thread());
        } else {
            log.error("[{}] FAILED    | {} | {}", request.id(), Thread.currentThread().getName(), error.toString(), error);
        }
        return CompletableFuture.failedFuture(mapped);
    }

    // ------------------------------------------------------------------ dùng chung
    private CallTrace callCvParse(String mode, String cvId, RequestInfo request) {
        String executedOn = Thread.currentThread().getName();
        String correlationId = currentCorrelationId();
        long queueMs = System.currentTimeMillis() - request.receivedAt();
        log.info("[{}] START     | {} -> {} | cho trong hang doi {}ms | header doc tu ThreadLocal: {}",
                request.id(), request.thread(), executedOn, queueMs, correlationId);

        long start = System.currentTimeMillis();
        String result = cvParseClient.analyze(cvId);
        long callMs = System.currentTimeMillis() - start;
        log.info("[{}] DONE      | {} | parse CV {}ms | tong {}ms",
                request.id(), executedOn, callMs, queueMs + callMs);

        return new CallTrace(mode, request.id(), request.thread(), executedOn, correlationId,
                result, queueMs, callMs);
    }

    /**
     * Đọc header từ request hiện tại. RequestContextHolder gắn với ThreadLocal của thread Tomcat,
     * nên sang thread của THREADPOOL bulkhead sẽ KHÔNG còn -> minh hoạ việc mất context
     * (MDC/traceId, SecurityContext, header cần forward... đều bị mất tương tự).
     */
    private static String currentCorrelationId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader("X-Correlation-Id");
        }
        return "<khong co request context>";
    }
}
