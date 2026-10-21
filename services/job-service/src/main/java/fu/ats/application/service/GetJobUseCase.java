package fu.ats.application.service;

import fu.ats.application.port.in.GetJobPort;
import fu.ats.domain.aggregate.JobAggregate;
import fu.ats.domain.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetJobUseCase implements GetJobPort {
    private final JobRepository jobRepository;

    // readOnly tx: skillIds is a lazy @ElementCollection, mapper touches it (open-in-view=false)
    @Override
    @Transactional(readOnly = true)
    public JobAggregate execute(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found with id: " + jobId));
    }
}
