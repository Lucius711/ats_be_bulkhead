package fu.ats.service;

import fu.ats.dto.CandidateRequest;
import fu.ats.dto.CandidateResponse;
import fu.ats.entity.Candidates;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface CandidateService {
    Candidates save(CandidateRequest request);

    @Transactional(readOnly = true)
    CandidateResponse findById(UUID candidateId);
}
