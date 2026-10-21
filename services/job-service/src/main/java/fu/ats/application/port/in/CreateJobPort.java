package fu.ats.application.port.in;

import fu.ats.api.dto.JobResponse;
import fu.ats.application.command.JobCommand;
import fu.ats.domain.aggregate.JobAggregate;

public interface CreateJobPort {
    JobAggregate execute(JobCommand jobCommand);
}
