package fu.ats.domain.repository;

import fu.ats.api.dto.JobResponse;
import fu.ats.domain.aggregate.JobAggregate;
import fu.ats.infrastructure.persistence.entity.Job;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    JobAggregate save(JobAggregate aggregate);

    Optional<JobAggregate> findById(UUID id);
}
