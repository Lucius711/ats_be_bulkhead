package fu.ats.api.rest;

import fu.ats.api.dto.CreateJobRequest;
import fu.ats.api.dto.CustomUserDetails;
import fu.ats.api.dto.JobApplicationResponse;
import fu.ats.application.command.JobCommand;
import fu.ats.application.port.in.CreateJobPort;
import fu.ats.domain.aggregate.JobAggregate;
import fu.ats.infrastructure.persistence.entity.JobStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {
    @Value("${server.port}") // SpEL
    private String port;

    private final CreateJobPort createJobPort;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> createJob(@Valid @RequestBody CreateJobRequest request,
                                       @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Creating job for authenticated user {}", currentUser.getUserId());
        log.debug("Create job request {}", request);
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
    @PreAuthorize("hasAnyRole('RECRUITER', 'CANDIDATE')")
    public ResponseEntity<JobApplicationResponse> getJob(
            @Valid @PathVariable("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        log.info("getJob {} - {}", port, id);
        log.info("getJob user {} - {}", currentUser.getUserId(), currentUser.getUsername());
        log.info("getJob authorities {} - {}", currentUser.getAuthorities(), id);

        return ResponseEntity.ok(new JobApplicationResponse(id, "Fullstack Java Dev", JobStatus.PUBLISHED.toString(), LocalDate.of(2026, 12, 31)));
    }
}
