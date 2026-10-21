package fu.ats.application.port.in;

import fu.ats.domain.aggregate.JobAggregate;

import java.util.UUID;

public interface GetJobPort {
    JobAggregate execute(UUID jobId);
}
