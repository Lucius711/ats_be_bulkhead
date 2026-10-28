package fu.ats.bulkhead;

import fu.ats.config.CvParseProperties;
import fu.ats.engine.AnalysisEngine;
import fu.ats.engine.Workload;
import fu.ats.shared.commonweb.messaging.rabbitmq.RabbitMessagingNames;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trạng thái bulkhead của cv-parse-service theo thời gian thực (trang giám sát demo đọc API này).
 *
 *   GET /api/v1/cvparse/bulkhead        -> engine, 2 ngăn bulkhead, hàng đợi RabbitMQ
 *   PUT /api/v1/cvparse/bulkhead/mode   -> {"enabled": true|false} (chỉ khi toggle-enabled=true)
 */
@RestController
@RequestMapping("/api/v1/cvparse/bulkhead")
@RequiredArgsConstructor
public class BulkheadMonitorController {

    private final BulkheadMode mode;
    private final AnalysisEngine engine;
    private final CvParseExecutor executor;
    private final CvParseProperties properties;
    private final BulkheadRegistry bulkheadRegistry;
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final AmqpAdmin amqpAdmin;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public Map<String, Object> status() {
        ThreadPoolBulkhead.Metrics batch = threadPoolBulkheadRegistry.bulkhead(BulkheadNames.BATCH).getMetrics();
        Bulkhead interactive = bulkheadRegistry.bulkhead(BulkheadNames.INTERACTIVE);
        QueueInformation parseQueue = amqpAdmin.getQueueInfo(RabbitMessagingNames.CV_PARSE_QUEUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bulkheadEnabled", mode.enabled());
        body.put("toggleEnabled", mode.toggleEnabled());
        body.put("engine", Map.of(
                "capacity", engine.capacity(),
                "batchRunning", engine.running(Workload.BATCH),
                "batchWaiting", engine.waiting(Workload.BATCH),
                "interactiveRunning", engine.running(Workload.INTERACTIVE),
                "interactiveWaiting", engine.waiting(Workload.INTERACTIVE),
                "parseDelayMs", properties.engine().parseDelay().toMillis(),
                "matchDelayMs", properties.engine().matchDelay().toMillis()
        ));
        body.put("batchBulkhead", Map.of(
                "maxThreads", batch.getMaximumThreadPoolSize(),
                "activeThreads", batch.getActiveThreadCount(),
                "queueDepth", batch.getQueueDepth(),
                "queueCapacity", batch.getQueueCapacity()
        ));
        body.put("interactiveBulkhead", Map.of(
                "maxConcurrentCalls", interactive.getBulkheadConfig().getMaxConcurrentCalls(),
                "availableConcurrentCalls", interactive.getMetrics().getAvailableConcurrentCalls()
        ));
        body.put("unboundedActive", executor.unboundedActive());
        body.put("rabbitParseQueueReady", parseQueue == null ? -1 : parseQueue.getMessageCount());
        body.put("prefetch", properties.batch().prefetch());
        return body;
    }

    @PutMapping("/mode")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> setMode(@RequestBody Map<String, Boolean> request) {
        if (!mode.toggleEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "TOGGLE_DISABLED", "message", "Set cv-parse.bulkhead.toggle-enabled=true to switch at runtime"));
        }
        mode.set(Boolean.TRUE.equals(request.get("enabled")));
        return ResponseEntity.ok(Map.of("bulkheadEnabled", mode.enabled()));
    }
}
