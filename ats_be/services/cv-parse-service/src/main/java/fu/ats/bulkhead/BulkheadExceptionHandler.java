package fu.ats.bulkhead;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

/** Ngăn API đồng bộ đã đầy -> trả 503 ngay, kèm lời nhắn thử lại, thay vì để HR chờ mãi. */
@RestControllerAdvice
public class BulkheadExceptionHandler {

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<Map<String, Object>> handleBulkheadFull(BulkheadFullException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "code", "CV_PARSE_BUSY",
                        "message", "CV analysis is busy, please retry in a few seconds",
                        "detail", exception.getMessage(),
                        "timestamp", OffsetDateTime.now().toString()
                ));
    }
}
