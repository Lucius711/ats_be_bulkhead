package fu.ats.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

public record SalaryRange(BigDecimal min, BigDecimal max) {
    public SalaryRange {
        Objects.requireNonNull(min, "salaryMin is required");
        Objects.requireNonNull(max, "salaryMax is required");

        if (min.signum() < 0 || max.signum() < 0) {
            throw new IllegalArgumentException("Salary cannot be negative");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("salaryMin cannot be greater than salaryMax");
        }
    }

    public static SalaryRange of(BigDecimal min, BigDecimal max) {
        return new SalaryRange(min, max);
    }
}
