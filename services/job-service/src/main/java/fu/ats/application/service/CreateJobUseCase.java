package fu.ats.application.service;

import fu.ats.api.dto.JobResponse;
import fu.ats.application.command.JobCommand;
import fu.ats.application.port.in.CreateJobPort;
import fu.ats.domain.aggregate.JobAggregate;
import fu.ats.domain.repository.JobRepository;
import fu.ats.infrastructure.persistence.entity.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateJobUseCase implements CreateJobPort {
    private final JobRepository jobRepository;
    @Override
    public JobAggregate execute(JobCommand command) {

        // map cmd -> aggregate
        if (command == null) {
            throw new IllegalArgumentException("Job command must not be null");
        }

        JobAggregate aggregate = JobAggregate.createDraft(
                null,
                command.title(),
                command.description(),
                command.departmentId(),
                command.recruiterId(),
                command.location(),
                command.employmentType(),
                command.workMode(),
                command.salaryMin(),
                command.salaryMax(),
                command.currency(),
                command.applicationDeadline(),
                command.skillIds()
        );

        return jobRepository.save(aggregate);
    }

    private Job toEntity(JobAggregate aggregate) {
        return Job.builder()
                .id(aggregate.getId())
                .title(aggregate.getTitle())
                .description(aggregate.getDescription())
                .location(aggregate.getLocation())
                .recruiterId(aggregate.getRecruiterId())
                .salaryMin(aggregate.getSalaryRange().min())
                .salaryMax(aggregate.getSalaryRange().max())
                .deadline(aggregate.getDeadline())
                .status(
                        fu.ats.infrastructure.persistence.entity.JobStatus
                                .valueOf(aggregate.getStatus().name())
                )
                .build();
    }
}
