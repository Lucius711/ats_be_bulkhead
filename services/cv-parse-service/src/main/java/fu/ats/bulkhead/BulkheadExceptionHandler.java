package fu.ats.bulkhead;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Dung chung cho ca Semaphore va ThreadPool Bulkhead: het cho -> 503 (chinh sach cua app, khong phai mac dinh R4j). */
@Slf4j
@RestControllerAdvice
public class BulkheadExceptionHandler {

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<Map<String, String>> handleFull(BulkheadFullException ex) {
        log.warn("xxx REJECTED: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "REJECTED",
                        "message", "He thong phan tich CV dang ban, vui long thu lai sau"));
    }
}
