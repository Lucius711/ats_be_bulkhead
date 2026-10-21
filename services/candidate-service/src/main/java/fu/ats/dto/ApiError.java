package fu.ats.dto;

import java.time.OffsetDateTime;

public record ApiError(
        String code,
        String message,
        OffsetDateTime timestamp,
        String path,
        String traceId
) {
}