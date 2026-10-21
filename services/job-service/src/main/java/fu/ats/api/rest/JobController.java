package fu.ats.api.rest;

import fu.ats.api.dto.CreateJobRequest;
import fu.ats.api.dto.JobApplicationResponse;
import fu.ats.application.command.JobCommand;
import fu.ats.application.port.in.CreateJobPort;
import fu.ats.application.port.in.GetJobPort;
import fu.ats.domain.aggregate.JobAggregate;
import jakarta.validation.Valid;
import jakarta.ws.rs.HeaderParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {
    @Value("${server.port}") // SpEL
    private String port;

    private final CreateJobPort createJobPort;
    private final GetJobPort getJobPort;

    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody CreateJobRequest request) {

        JobAggregate jobAggregate = createJobPort.execute(new JobCommand(
                request.title(),
                request.description(),
                request.departmentId(),
                request.recruiterId(),
                request.location(),
                request.employmentType(),
                request.workMode(),
                request.salaryMin(),
                request.salaryMax(),
                request.currency(),
                request.applicationDeadline(),
                request.skillIds()));

        return ResponseEntity.ok(jobAggregate);

    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getJob(@Valid @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        log.info("getJob {} - {}", port, id);
        log.info("getJob {} - {}", userId, email);

        return ResponseEntity.ok(new JobApplicationResponse(id, "Fullstack Java Dev", JobStatus.PUBLISHED.toString(),
                LocalDate.of(2026, 12, 31)));
    }
}
