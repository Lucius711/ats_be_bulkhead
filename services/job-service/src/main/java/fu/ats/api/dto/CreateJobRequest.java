package fu.ats.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateJobRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String description,
        @NotNull Long recruiterId,
        @NotNull UUID departmentId,
        @NotBlank @Size(max = 255) String location,
        @NotBlank String employmentType,
        @NotBlank String workMode,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal salaryMin,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal salaryMax,
        @NotBlank @Size(max = 3) String currency,
        @NotNull @Future LocalDate applicationDeadline,
        @Size(max = 50) List<UUID> skillIds
) {
}
