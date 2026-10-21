package fu.ats.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID jobId,
        String title,
        String description,
        UUID companyId,
        UUID recruiterId,
        String location,
        String employmentType,
        String workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        String status,
        LocalDate applicationDeadline,
        List<UUID> skillIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
