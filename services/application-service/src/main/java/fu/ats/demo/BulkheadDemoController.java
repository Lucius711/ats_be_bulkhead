package fu.ats.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Endpoint phục vụ demo (gọi thẳng application-service :8083, không qua gateway).
 *
 *   GET /api/v1/applications/demo/cv-parse/{cvId}/no-bulkhead
 *   GET /api/v1/applications/demo/cv-parse/{cvId}/thread-pool
 *   GET /api/v1/applications/demo/ping   <- endpoint "vô can", không gọi downstream.
 *                                           Dùng để đo: cv-parse-service chậm có kéo cả service chết theo không?
 *
 * Mỗi request được log theo vòng đời: RECEIVED -> START -> DONE, hoặc RECEIVED -> REJECTED.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applications/demo")
@Slf4j
public class BulkheadDemoController {

    private final BulkheadDemoService demoService;

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "pong", LocalTime.now().toString(),
                "thread", Thread.currentThread().getName());
    }

    @GetMapping("/cv-parse/{cvId}/no-bulkhead")
    public CallTrace noBulkhead(@PathVariable("cvId") String cvId,
                                @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        RequestInfo request = received(correlationId);
        return demoService.withoutBulkhead(cvId, request);
    }

    /**
     * Trả về CompletableFuture -> Spring MVC xử lý bất đồng bộ:
     * thread Tomcat được TRẢ VỀ pool ngay, response được ghi khi bulkhead thread chạy xong.
     */
    @GetMapping("/cv-parse/{cvId}/thread-pool")
    public CompletableFuture<CallTrace> threadPool(@PathVariable("cvId") String cvId,
                                                   @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        RequestInfo request = received(correlationId);
        return demoService.withThreadPool(cvId, request);
    }

    private static RequestInfo received(String correlationId) {
        RequestInfo request = RequestInfo.received(correlationId);
        log.info("[{}] RECEIVED  | {}", request.id(), request.thread());
        return request;
    }
}
