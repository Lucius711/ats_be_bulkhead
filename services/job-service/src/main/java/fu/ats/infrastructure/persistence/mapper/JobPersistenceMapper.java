package fu.ats.infrastructure.persistence.mapper;

import fu.ats.domain.aggregate.JobAggregate;
import fu.ats.domain.model.JobStatus;
import fu.ats.infrastructure.persistence.entity.Department;
import fu.ats.infrastructure.persistence.entity.Job;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Hand-written because JobAggregate is only created through its factories
 * (createDraft / restore), which MapStruct cannot call.
 */
@Component
public class JobPersistenceMapper {

    public Job toEntity(JobAggregate aggregate, Department department) {
        Job job = Job.builder()
                .id(aggregate.getId())
                .recruiterId(aggregate.getRecruiterId())
                .title(aggregate.getTitle())
                .description(aggregate.getDescription())
                .location(aggregate.getLocation())
                .employmentType(aggregate.getEmploymentType())
                .workMode(aggregate.getWorkMode())
                .salaryMin(aggregate.getSalaryRange().min())
                .salaryMax(aggregate.getSalaryRange().max())
                .currency(aggregate.getCurrency())
                .status(fu.ats.infrastructure.persistence.entity.JobStatus
                        .valueOf(aggregate.getStatus().name()))
                .deadline(aggregate.getDeadline())
                .skillIds(new ArrayList<>(aggregate.getSkillIds()))
                .createdAt(aggregate.getCreatedAt())
                .updatedAt(aggregate.getUpdatedAt())
                .build();

        if (department != null) {
            job.setDepartment(department);
        }

        return job;
    }

    public JobAggregate toDomain(Job entity) {
        return JobAggregate.restore(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDepartment().getId(),
                entity.getRecruiterId(),
                entity.getLocation(),
                entity.getEmploymentType(),
                entity.getWorkMode(),
                entity.getSalaryMin(),
                entity.getSalaryMax(),
                entity.getCurrency(),
                entity.getDeadline(),
                entity.getSkillIds(),
                JobStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
