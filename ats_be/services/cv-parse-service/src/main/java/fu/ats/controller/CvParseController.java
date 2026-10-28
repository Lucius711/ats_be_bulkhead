package fu.ats.controller;

import fu.ats.bulkhead.CvParseExecutor;
import fu.ats.config.CvParseProperties;
import fu.ats.dto.CvAnalysisRequest;
import fu.ats.dto.CvAnalysisResponse;
import fu.ats.dto.CvMatchRequest;
import fu.ats.dto.CvMatchResponse;
import fu.ats.engine.AnalysisEngine;
import fu.ats.engine.Workload;
import fu.ats.service.CvParseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cvparse")
@RequiredArgsConstructor
public class CvParseController {

    private final CvParseService cvParseService;
    private final CvParseExecutor cvParseExecutor;
    private final AnalysisEngine engine;
    private final CvParseProperties properties;

    // API đồng bộ (người dùng đang chờ): đi qua ngăn Semaphore "cvParseInteractive",
    // được giữ riêng phần năng lực engine nên không bị đợt parse hàng loạt chiếm mất.

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public CvAnalysisResponse analyze(
            @Valid @RequestBody CvAnalysisRequest request
    ) {
        return cvParseExecutor.interactive(() -> engine.run(
                Workload.INTERACTIVE,
                properties.engine().parseDelay(),
                () -> cvParseService.analyze(request)
        ));
    }

    @PostMapping("/match")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public CvMatchResponse match(
            @Valid @RequestBody CvMatchRequest request
    ) {
        return cvParseExecutor.interactive(() -> engine.run(
                Workload.INTERACTIVE,
                properties.engine().matchDelay(),
                () -> cvParseService.match(request)
        ));
    }
}
