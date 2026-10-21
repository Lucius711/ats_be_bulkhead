package fu.ats.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record JobApplicationResponse (
    UUID jobId,
    String title,
    String status,
    LocalDate applicationDeadline)
{
}
