package fu.ats.exception;

import fu.ats.dto.ApiError;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        ex.printStackTrace();
        log.error(ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        "APPLICATION_NOT_FOUND",
                        ex.getMessage(),
                        OffsetDateTime.now(),
                        request.getRequestURI()));
    }

    /** Lưới an toàn: BulkheadFullException lọt ra mà không qua fallback -> vẫn trả 503 thay vì 500. */
    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<ApiError> handleBulkheadFull(BulkheadFullException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("DEPENDENCY_BUSY", ex.getMessage(), OffsetDateTime.now(), request.getRequestURI()));
    }

    @ExceptionHandler(DownstreamException.class)
    public ResponseEntity<ApiError> handleDownstreamException(
            DownstreamException ex,
            HttpServletRequest request) {
        HttpStatus status;
        String code;
        String message;

        switch (ex.getErrorCode()) {
            case "CANDIDATE_NOT_FOUND" -> {
                status = HttpStatus.NOT_FOUND;
                code = "APPLICATION_CANDIDATE_NOT_FOUND";
                message = "The requested candidate does not exist";
            }
            case "JOB_NOT_FOUND" -> {
                status = HttpStatus.NOT_FOUND;
                code = "APPLICATION_JOB_NOT_FOUND";
                message = "The requested job does not exist";
            }
            case ServiceBusyException.ERROR_CODE -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                code = "DEPENDENCY_BUSY";
                message = ex.getService() + " is overloaded, request rejected by bulkhead. Please retry later";
            }
            case "JOB_CLOSED" -> {
                status = HttpStatus.CONFLICT;
                code = "APPLICATION_JOB_CLOSED";
                message = "The job is no longer accepting applications";
            }
            default -> {
                status = HttpStatus.BAD_GATEWAY;
                code = "DEPENDENCY_ERROR";
                message = "A required service returned an invalid response";
            }
        }

        return ResponseEntity.status(status)
                .body(new ApiError(
                        code,
                        message,
                        OffsetDateTime.now(),
                        request.getRequestURI()));
    }
}
