package fu.ats.domain.aggregate;

import fu.ats.domain.model.JobStatus;
import fu.ats.domain.valueobject.SalaryRange;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class JobAggregate {
    private final UUID id;
    private final UUID departmentId;
    private final Long recruiterId;
    private final String title;
    private final String description;
    private final String location;
    private final String employmentType;
    private final String workMode;
    private final String currency;
    private final SalaryRange salaryRange;
    private final LocalDate deadline;
    private final List<UUID> skillIds;
    private JobStatus status;
    private OffsetDateTime createdAt;
    private  OffsetDateTime updatedAt;

    private JobAggregate(UUID id,
                         String title,
                         String description,
                         UUID departmentId,
                         Long recruiterId,
                         String location,
                         String employmentType,
                         String workMode,
                         BigDecimal salaryMin,
                         BigDecimal salaryMax,
                         String currency,
                         LocalDate deadline,
                         List<UUID> skillIds

    ) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId is required");
        this.recruiterId = Objects.requireNonNull(recruiterId, "recruiterId is required");
        this.location = requireText(location, "location");
        this.employmentType = requireText(employmentType, "employmentType");
        this.workMode = requireText(workMode, "workMode");
        this.currency = requireText(currency, "currency");
        this.salaryRange = SalaryRange.of(salaryMin, salaryMax);
        this.deadline = Objects.requireNonNull(deadline, "deadline is required");
        this.skillIds = skillIds == null ? List.of() : List.copyOf(skillIds);
        this.status = JobStatus.DRAFT;
    }

    private JobAggregate(UUID id,
                          String title,
                          String description,
                          UUID departmentId,
                          Long recruiterId,
                          String location,
                          String employmentType,
                          String workMode,
                          BigDecimal salaryMin,
                          BigDecimal salaryMax,
                          String currency,
                          LocalDate deadline,
                          List<UUID> skillIds,
                         JobStatus status,
                         OffsetDateTime createdAt,
                         OffsetDateTime updatedAt

    ) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId is required");
        this.recruiterId = Objects.requireNonNull(recruiterId, "recruiterId is required");
        this.location = requireText(location, "location");
        this.employmentType = requireText(employmentType, "employmentType");
        this.workMode = requireText(workMode, "workMode");
        this.currency = requireText(currency, "currency");
        this.salaryRange = SalaryRange.of(salaryMin, salaryMax);
        this.deadline = Objects.requireNonNull(deadline, "deadline is required");
        this.skillIds = skillIds == null ? List.of() : List.copyOf(skillIds);
        this.status = JobStatus.DRAFT;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
    }

    public static JobAggregate createDraft(UUID id,
                                            String title,
                                            String description,
                                            UUID departmentId,
                                            Long recruiterId,
                                            String location,
                                            String employmentType,
                                            String workMode,
                                            BigDecimal salaryMin,
                                            BigDecimal salaryMax,
                                            String currency,
                                            LocalDate deadline,
                                            List<UUID> skillIds) {
        return new JobAggregate(
                id,
                title,
                description,
                departmentId,
                recruiterId,
                location,
                employmentType,
                workMode,
                salaryMin,
                salaryMax,
                currency,
                deadline,
                skillIds
        );
    }

    public void publish() {
        ensureStatus(JobStatus.DRAFT);
        this.status = JobStatus.PUBLISHED;
    }

    public void close() {
        if (this.status != JobStatus.PUBLISHED) {
            throw new IllegalStateException("Only a published job can be closed");
        }
        this.status = JobStatus.CLOSED;
    }

    private void ensureStatus(JobStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException(
                    "Job must be " + expectedStatus + " but was " + this.status
            );
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public static JobAggregate restore(UUID id,
                                       String title,
                                       String description,
                                       UUID departmentId,
                                       Long recruiterId,
                                       String location,
                                       String employmentType,
                                       String workMode,
                                       BigDecimal salaryMin,
                                       BigDecimal salaryMax,
                                       String currency,
                                       LocalDate deadline,
                                       List<UUID> skillIds,
                                       JobStatus status,
                                       OffsetDateTime createdAt,
                                       OffsetDateTime updatedAt) {
        return new JobAggregate(
                Objects.requireNonNull(id, "id is required"),
                title,
                description,
                departmentId,
                recruiterId,
                location,
                employmentType,
                workMode,
                salaryMin,
                salaryMax,
                currency,
                deadline,
                skillIds,
                status,
                createdAt,
                updatedAt
        );
    }
}
