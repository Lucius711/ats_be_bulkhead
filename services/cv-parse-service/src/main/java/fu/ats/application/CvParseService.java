package fu.ats.application;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class CvParseService {

    private final io.github.resilience4j.bulkhead.Bulkhead bulkhead;
    private final int max;
    private final AtomicInteger runningNoBulkhead = new AtomicInteger(); // dem that so CV dang o trong parseWithoutBulkhead

    public CvParseService(BulkheadRegistry bulkheadRegistry) {
        this.bulkhead = bulkheadRegistry.bulkhead("cvParse");
        this.max = bulkhead.getBulkheadConfig().getMaxConcurrentCalls();
        bulkhead.getEventPublisher()
                .onCallPermitted(e -> log.info("[{}] (2) >>> VAO: nhan 1 permit | dang chay {}/{}", MDC.get("cvId"), running(), max))
                .onCallRejected(e -> log.warn("[{}] (x) XXX TU CHOI: het thoi gian cho ma khong co permit | dang chay {}/{}", MDC.get("cvId"), running(), max));
    }

    @Bulkhead(name = "cvParse", type = Bulkhead.Type.SEMAPHORE)
    public long parse(String cvId) throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread.sleep(8000);
        log.info("[{}] (3) <<< RA : xu ly xong, tra 1 permit | truoc khi tra: dang chay {}/{}", cvId, running(), max);
        return System.currentTimeMillis() - start;
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
}
