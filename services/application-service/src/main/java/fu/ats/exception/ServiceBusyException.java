package fu.ats.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Bulkhead đã đầy -> từ chối lời gọi sang downstream để bảo vệ application-service.
 * Kế thừa DownstreamException để đi chung luồng xử lý lỗi sẵn có (GlobalExceptionHandler -> HTTP 503).
 */
public class ServiceBusyException extends DownstreamException {

    public static final String ERROR_CODE = "BULKHEAD_FULL";

    public ServiceBusyException(String service, String message) {
        super(service, 503, ERROR_CODE, message);
    }

    /**
     * Dùng trong fallbackMethod của @Bulkhead:
     * - nếu nguyên nhân là BulkheadFullException -> đổi thành ServiceBusyException (503)
     * - lỗi khác (404 candidate, 5xx...) -> ném lại nguyên trạng, KHÔNG nuốt lỗi nghiệp vụ
     */
    public static RuntimeException fromBulkhead(String service, Throwable error) {
        Throwable root = error;
        while ((root instanceof CompletionException || root instanceof ExecutionException)
                && root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof BulkheadFullException full) {
            return new ServiceBusyException(service, full.getMessage());
        }
        if (root instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException(root);
    }
}
