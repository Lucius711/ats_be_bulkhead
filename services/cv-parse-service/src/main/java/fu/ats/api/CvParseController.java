package fu.ats.api;

import fu.ats.application.CvParseService;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/cvparses")
@RequiredArgsConstructor
public class CvParseController {

    private final CvParseService cvParseService;
    private final BulkheadRegistry bulkheadRegistry;

    @GetMapping("/analyze/{cvId}")
    public ResponseEntity<String> analyze(@PathVariable("cvId") String cvId) throws InterruptedException {
        var bulkhead = bulkheadRegistry.bulkhead("cvParse");
        int max = bulkhead.getBulkheadConfig().getMaxConcurrentCalls();
        long maxWait = bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis();
        long start = System.currentTimeMillis();
        MDC.put("cvId", cvId);

        log.info("[{}] (1) DEN SERVICE, xin permit (bulkhead cvParse: toi da {} permit, cho toi da {} ms)", cvId, max, maxWait);
        try {
            long processMs = cvParseService.parse(cvId);
            long total = System.currentTimeMillis() - start;
            String msg = String.format("%s XU LY XONG sau %d ms = cho permit %d ms + xu ly %d ms",
                    cvId, total, total - processMs, processMs);
            log.info("[{}] (4) TRA VE 200 | {}", cvId, msg);
            return ResponseEntity.ok(msg);
        } catch (BulkheadFullException e) {
            String msg = String.format("BULKHEAD: %s BI TU CHOI sau khi cho %d ms - ca %d permit van dang bi cac CV khac giu (chua xu ly xong), qua maxWaitDuration %d ms",
                    cvId, System.currentTimeMillis() - start, max, maxWait);
            log.warn("[{}] (4) TRA VE 503 | {}", cvId, msg);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(msg);
        } finally {
            MDC.remove("cvId");
        }
    }

    @GetMapping("/analyze-no-bulkhead/{cvId}")
    public ResponseEntity<String> analyzeNoBulkhead(@PathVariable("cvId") String cvId) throws InterruptedException {
        long start = System.currentTimeMillis();
        log.info("[{}] (1) DEN SERVICE (KHONG co bulkhead, khong can permit)", cvId);
        cvParseService.parseWithoutBulkhead(cvId);
        String msg = String.format("%s XU LY XONG sau %d ms (khong co bulkhead: khong cho, khong gioi han)", cvId, System.currentTimeMillis() - start);
        log.info("[{}] (4) TRA VE 200 | {}", cvId, msg);
        return ResponseEntity.ok(msg);
    }
}
