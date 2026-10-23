package fu.ats.demo;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Actuator endpoint cho trang dashboard /bulkhead-demo.html (chỉ bật với profile demo).
 *
 *   POST /actuator/bulkheaddemo  {"mode":"semaphore","n":40}  -> bắn n request parse CV đồng thời vào chính service
 *   GET  /actuator/bulkheaddemo                               -> trạng thái thread Tomcat / Semaphore / ThreadPool + kết quả lượt chạy
 *
 * Chạy trên cổng actuator riêng (8086) nên vẫn trả lời được khi Tomcat chính (8085) đã cạn thread.
 * Trình duyệt chỉ mở ~6 kết nối song song tới 1 host, nên việc bắn tải phải làm ở phía server.
 */
@Component
@Profile("demo")
@Endpoint(id = "bulkheaddemo")
@RequiredArgsConstructor
public class BulkheadDemoEndpoint {

    private static final Map<String, String> ENDPOINTS = Map.of(
            "no-bulkhead", "analyze-no-bulkhead",
            "semaphore", "analyze",
            "thread-pool", "analyze-threadpool");

    private final Environment environment;
    private final MeterRegistry meterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private volatile Run current;

    @WriteOperation
    public Map<String, Object> start(String mode, int n) {
        String path = ENDPOINTS.get(mode);
        if (path == null) {
            return Map.of("error", "mode phai la mot trong " + ENDPOINTS.keySet());
        }
        if (current != null && current.isRunning()) {
            return Map.of("error", "Luot " + current.mode + " dang chay, doi xong da");
        }
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8085"));
        String base = "http://localhost:" + port + "/api/v1/cvparses/" + path + "/CV-";

        Run run = new Run(mode, n);
        current = run;
        for (int i = 0; i < n; i++) {
            int index = i;
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + (i + 1)))
                    .timeout(Duration.ofSeconds(90))
                    .GET()
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, error) -> run.finish(index, error == null ? response.statusCode() : -1));
        }
        return Map.of("mode", mode, "n", n);
    }

    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("live", live());
        Run run = current;
        if (run != null) {
            body.put("run", run.snapshot());
        }
        return body;
    }

    private Map<String, Object> live() {
        Bulkhead semaphore = bulkheadRegistry.bulkhead("cvParse");
        ThreadPoolBulkhead.Metrics pool = threadPoolBulkheadRegistry.bulkhead("cvParsePool").getMetrics();
        int semaMax = semaphore.getBulkheadConfig().getMaxConcurrentCalls();

        Map<String, Object> live = new LinkedHashMap<>();
        live.put("tomcatBusy", gauge("tomcat.threads.busy"));
        live.put("tomcatMax", gauge("tomcat.threads.config.max"));
        live.put("semaUsed", semaMax - semaphore.getMetrics().getAvailableConcurrentCalls());
        live.put("semaMax", semaMax);
        live.put("poolActive", pool.getActiveThreadCount());
        live.put("poolMax", pool.getMaximumThreadPoolSize());
        live.put("queue", pool.getQueueDepth());
        live.put("queueMax", pool.getQueueCapacity());
        return live;
    }

    private int gauge(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge == null ? -1 : (int) gauge.value();
    }

    /** 1 lượt bắn tải: trạng thái + thời gian phản hồi của từng CV. */
    private static final class Run {
        private final String mode;
        private final int n;
        private final long startedAt = System.currentTimeMillis();
        private final AtomicIntegerArray status;   // 0 = đang chờ phản hồi, -1 = lỗi kết nối
        private final AtomicLongArray elapsedMs;
        private final AtomicInteger done = new AtomicInteger();

        private Run(String mode, int n) {
            this.mode = mode;
            this.n = n;
            this.status = new AtomicIntegerArray(n);
            this.elapsedMs = new AtomicLongArray(n);
        }

        private void finish(int index, int code) {
            elapsedMs.set(index, System.currentTimeMillis() - startedAt);
            status.set(index, code);
            done.incrementAndGet();
        }

        private boolean isRunning() {
            return done.get() < n;
        }

        private Map<String, Object> snapshot() {
            List<List<Long>> results = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                results.add(List.of((long) status.get(i), elapsedMs.get(i)));
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("mode", mode);
            snapshot.put("n", n);
            snapshot.put("running", isRunning());
            snapshot.put("elapsedMs", System.currentTimeMillis() - startedAt);
            snapshot.put("results", results);   // [httpStatus, ms]
            return snapshot;
        }
    }
}
