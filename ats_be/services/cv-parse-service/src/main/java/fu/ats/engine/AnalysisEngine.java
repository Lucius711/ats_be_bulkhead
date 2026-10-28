package fu.ats.engine;

import fu.ats.config.CvParseProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Mô phỏng engine phân tích CV (OCR/AI) có năng lực giới hạn: chỉ {@code capacity}
 * việc được chạy cùng lúc, việc tới sau phải xếp hàng (công bằng, ai tới trước chạy trước).
 *
 * <p>Đây là tài nguyên DÙNG CHUNG giữa parse CV hàng loạt (RabbitMQ) và API đồng bộ của HR.
 * Không có bulkhead thì đợt CV hàng loạt chiếm hết engine, HR phải xếp hàng phía sau.</p>
 */
@Component
public class AnalysisEngine {

    private final int capacity;
    private final Semaphore slots;
    private final Map<Workload, AtomicInteger> running = new EnumMap<>(Workload.class);
    private final Map<Workload, AtomicInteger> waiting = new EnumMap<>(Workload.class);

    public AnalysisEngine(CvParseProperties properties) {
        this.capacity = properties.engine().capacity();
        this.slots = new Semaphore(capacity, true);
        for (Workload workload : Workload.values()) {
            running.put(workload, new AtomicInteger());
            waiting.put(workload, new AtomicInteger());
        }
    }

    /** Chiếm 1 slot của engine, xử lý mất {@code cost}, rồi trả slot. */
    public <T> T run(Workload workload, Duration cost, Supplier<T> work) {
        waiting.get(workload).incrementAndGet();
        try {
            slots.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for analysis engine", exception);
        } finally {
            waiting.get(workload).decrementAndGet();
        }

        running.get(workload).incrementAndGet();
        try {
            if (!cost.isZero()) {
                Thread.sleep(cost.toMillis());
            }
            return work.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while analysing CV", exception);
        } finally {
            running.get(workload).decrementAndGet();
            slots.release();
        }
    }

    public int capacity() {
        return capacity;
    }

    public int running(Workload workload) {
        return running.get(workload).get();
    }

    public int waiting(Workload workload) {
        return waiting.get(workload).get();
    }
}
