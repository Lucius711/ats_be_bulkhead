package fu.ats.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateJobRequest(
        @Size(max = 255) String title,
        String description,
        @Size(max = 255) String location,
        String employmentType,
        String workMode,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal salaryMin,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal salaryMax,
        @Size(max = 3) String currency,
        LocalDate applicationDeadline,
        @Size(max = 50) List<UUID> skillIds
) {
}
