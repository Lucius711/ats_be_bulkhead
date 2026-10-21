package fu.ats.controller;

import fu.ats.dto.CandidateRequest;
import fu.ats.dto.CandidateResponse;
import fu.ats.entity.Candidates;
import fu.ats.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Candidates> createCandidate(
            @Valid @RequestBody CandidateRequest request
    ) {
        return ResponseEntity.ok(candidateService.save(request));
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<CandidateResponse> getCandidateById(
            @PathVariable("candidateId") UUID candidateId
    ) {
        log.info("getCandidateById {}", port, candidateId);

        return ResponseEntity.ok(candidateService.findById(candidateId));
    }
}
