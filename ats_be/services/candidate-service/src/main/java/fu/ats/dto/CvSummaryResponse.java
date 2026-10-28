package fu.ats.dto;

import fu.ats.entity.ParseStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Một CV trong danh sách cho HR: ai nộp, file nào, đã parse chưa và kết quả parse. */
public record CvSummaryResponse(
        Long cvId,
        UUID candidateId,
        String candidateName,
        String candidateEmail,
        String fileName,
        ParseStatus parseStatus,
        OffsetDateTime uploadedAt,
        OffsetDateTime parsedAt,
        Map<String, Object> parsedData
) {
}
