package fu.ats.service;

import fu.ats.dto.ApplicationRequest;
import fu.ats.dto.ApplicationResponse;
import fu.ats.dto.CandidateView;

import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse createApplication(ApplicationRequest request);

    ApplicationResponse getApplicationById(UUID applicationId);

    CandidateView validateCandidateBeforeOperation(UUID candidateId);
}
