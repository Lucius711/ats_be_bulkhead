package fu.ats.controller;

import fu.ats.dto.CandidateRequest;
import fu.ats.dto.CandidateResponse;
import fu.ats.dto.CvListResponse;
import fu.ats.entity.Candidates;
import fu.ats.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@Slf4j
public class CandidateController {
    @Value("${server.port}")
    private String port;

    private final CandidateService candidateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<Candidates> createCandidate(
            @Valid @RequestBody CandidateRequest request

    ) {
        return ResponseEntity.ok(candidateService.save(request));
    }

    // Recruiter thêm ứng viên và upload CV
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<CandidateResponse> createCandidateWithCv(
            @Valid @ModelAttribute CandidateRequest request,
            @RequestPart("cvFile") MultipartFile cvFile
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(candidateService.saveWithCv(request, cvFile));
    }

    /** CV mới nhất kèm trạng thái parse (PENDING/PARSED/FAILED) và kết quả parse, cho HR. */
    @GetMapping("/cvs")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public CvListResponse recentCvs(
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return candidateService.findRecentCvs(limit);
    }

    @GetMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<CandidateResponse> getCandidateById(
            @PathVariable("candidateId") UUID candidateId
    ) {
        log.info("getCandidateById port={}, candidateId={}", port, candidateId);

        return ResponseEntity.ok(candidateService.findById(candidateId));
    }
}
