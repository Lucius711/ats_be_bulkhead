package fu.ats.infrastructure.persistence.entity;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class SalaryRange {
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    private SalaryRange() {
    }

    private SalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
    }

    public static SalaryRange salaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMax == null || salaryMin == null) {
            throw new IllegalArgumentException("salaryMax or salaryMin is null");
        }

        if (salaryMin.compareTo(salaryMax) > 0) {
            throw new IllegalArgumentException("salaryMin > salaryMax");
        }

        return new SalaryRange(salaryMin, salaryMax);
    }

}
