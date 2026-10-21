package fu.ats.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JobCommand(String title,
                         String description,
                         UUID departmentId,
                         Long recruiterId,
                         String location,
                         String employmentType,
                         String workMode,
                         BigDecimal salaryMin,
                         BigDecimal salaryMax,
                         String currency,
                         LocalDate applicationDeadline,
                         List<UUID> skillIds
) {

}