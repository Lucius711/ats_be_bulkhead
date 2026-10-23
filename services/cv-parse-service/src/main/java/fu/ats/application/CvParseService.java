package fu.ats.application;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class CvParseService {

    private final io.github.resilience4j.bulkhead.Bulkhead bulkhead;
    private final int max;
    private final AtomicInteger runningNoBulkhead = new AtomicInteger(); // dem that so CV dang o trong parseWithoutBulkhead
    private final ThreadPoolBulkhead pool;

    public CvParseService(BulkheadRegistry bulkheadRegistry, ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry) {
        this.bulkhead = bulkheadRegistry.bulkhead("cvParse");
        this.max = bulkhead.getBulkheadConfig().getMaxConcurrentCalls();
        bulkhead.getEventPublisher()
                .onCallPermitted(e -> log.info("[{}] (2) >>> VAO: nhan 1 permit | dang chay {}/{}", MDC.get("cvId"), running(), max))
                .onCallRejected(e -> log.warn("[{}] (x) XXX TU CHOI: het thoi gian cho ma khong co permit | dang chay {}/{}", MDC.get("cvId"), running(), max));

        // Tu choi xay ra tren thread Tomcat luc submit -> con MDC cvId.
        // (Su kien "permitted" ban ra tren thread bulkhead nen mat MDC -> dung log (2b) trong parseOnThreadPool thay the.)
        this.pool = threadPoolBulkheadRegistry.bulkhead("cvParsePool");
        pool.getEventPublisher()
                .onCallRejected(e -> log.warn("[{}] (x) XXX TU CHOI NGAY: pool + hang doi day | {}", MDC.get("cvId"), poolState()));
    }

    @Bulkhead(name = "cvParse", type = Bulkhead.Type.SEMAPHORE)
    public long parse(String cvId) throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread.sleep(8000);
        log.info("[{}] (3) <<< RA : xu ly xong, tra 1 permit | truoc khi tra: dang chay {}/{}", cvId, running(), max);
        return System.currentTimeMillis() - start;
    }

    // ThreadPool: CV duoc parse tren pool rieng "bulkhead-cvParsePool-*", thread Tomcat duoc tra ve ngay.
    // Pool day + hang doi day -> tu choi NGAY (khong giu thread nao de cho).
    @Bulkhead(name = "cvParsePool", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Long> parseOnThreadPool(String cvId) throws InterruptedException {
        log.info("[{}] (2b) BAT DAU PARSE tren {} | {}", cvId, Thread.currentThread().getName(), poolState());
        long start = System.currentTimeMillis();
        Thread.sleep(8000);
        log.info("[{}] (3) <<< RA : xu ly xong tren {}", cvId, Thread.currentThread().getName());
        return CompletableFuture.completedFuture(System.currentTimeMillis() - start);
    }

    public long parseWithoutBulkhead(String cvId) throws InterruptedException {
        log.info("[{}] (2) >>> VAO NGAY (khong co bulkhead) | dang chay {} CV cung luc", cvId, runningNoBulkhead.incrementAndGet());
        long start = System.currentTimeMillis();
        Thread.sleep(8000);
        log.info("[{}] (3) <<< RA | con dang chay {} CV", cvId, runningNoBulkhead.decrementAndGet());
        return System.currentTimeMillis() - start;
    }

    private int running() {
        return max - bulkhead.getMetrics().getAvailableConcurrentCalls();
    }

    private String poolState() {
        ThreadPoolBulkhead.Metrics m = pool.getMetrics();
        return String.format("dang chay %d/%d | hang doi %d/%d",
                m.getActiveThreadCount(), m.getMaximumThreadPoolSize(), m.getQueueDepth(), m.getQueueCapacity());
    }
}
