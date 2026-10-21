package fu.ats.service.impl;

import fu.ats.client.candidate.CandidateClient;
import fu.ats.client.candidate.CandidateClient;
import fu.ats.client.job.JobClient;
import fu.ats.dto.ApplicationRequest;
import fu.ats.dto.ApplicationResponse;
import fu.ats.dto.CandidateView;
import fu.ats.dto.CandidateView;
import fu.ats.dto.JobView;
import fu.ats.entity.Application;
import fu.ats.entity.ApplicationStatus;
import fu.ats.entity.PipelineStage;
import fu.ats.exception.BusinessException;
import fu.ats.exception.ResourceNotFoundException;
import fu.ats.repository.ApplicationRepository;
import fu.ats.repository.PipelineStageRepository;
import fu.ats.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    private final PipelineStageRepository pipelineStageRepository;
    private final ApplicationRepository applicationRepository;
    private final JobClient jobClient;
    private final CandidateClient candidateClient;

    @Override
    @Transactional
    public ApplicationResponse createApplication(ApplicationRequest request) {
        UUID candidateId = request.getCandidateId();
        UUID jobId = request.getJobId();

        if (applicationRepository.findByCandidateIdAndJobId(candidateId, jobId).isPresent()) {
            throw new BusinessException(1, "Application already exists for this candidate and job");
        }

        CandidateView candidate = validateCandidateBeforeOperation(candidateId);
        JobView jobView = jobClient.findById(jobId);

        if (jobView.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException(2, "Application deadline is before application deadline");
        }

        log.info("Creating application for request {}", jobView);
        List<PipelineStage> pipelineStages = pipelineStageRepository.findByDefaultStage(true);

        Application application = new Application();
        application.setJobId(jobId);
        application.setCandidateId(candidateId);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setAppliedAt(OffsetDateTime.now());
        application.setPipelineStage(pipelineStages.get(0));

        application.setCvId(UUID.randomUUID());

        Application savedApplication = applicationRepository.save(application);
        return mapToResponse(savedApplication, candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));

        CandidateView candidate = candidateClient.findById(application.getCandidateId());
        return mapToResponse(application, candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateView validateCandidateBeforeOperation(UUID candidateId) {
        CandidateView candidate = candidateClient.findById(candidateId);

        if (!candidateId.equals(candidate.getId())) {
            throw new BusinessException(3, "Candidate ID does not match the requested candidate");
        }

        if (!"ACTIVE".equals(candidate.getStatus())) {
            throw new BusinessException(4, "Candidate is not active");
        }

        if (Boolean.TRUE.equals(candidate.getIsDuplicate())) {
            throw new BusinessException(5, "Candidate is marked as duplicate");
        }

        return candidate;
    }

    private ApplicationResponse mapToResponse(Application application, CandidateView candidate) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJobId())
                .candidateId(application.getCandidateId())
                .candidate(candidate)
                .cvId(application.getCvId())
                .departmentId(application.getDepartmentId())
                .transferredFrom(application.getTransferredFrom())
                .pipelineStageId(application.getPipelineStage() != null ? application.getPipelineStage().getId() : null)
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .build();
    }
}
