package fu.ats.service;

import fu.ats.dto.CandidateRequest;
import fu.ats.dto.CandidateResponse;
import fu.ats.dto.CvListResponse;
import fu.ats.entity.Candidates;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CandidateService {
    Candidates save(CandidateRequest request);

    @Transactional(readOnly = true)
    CandidateResponse findById(UUID candidateId);

    CandidateResponse saveWithCv(
            CandidateRequest request,
            MultipartFile cvFile
    );

    /** CV mới nhất (tối đa {@code limit}) kèm trạng thái parse, cho màn hình HR. */
    CvListResponse findRecentCvs(int limit);
}
