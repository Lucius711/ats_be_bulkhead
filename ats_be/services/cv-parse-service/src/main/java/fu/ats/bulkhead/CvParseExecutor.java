package fu.ats.bulkhead;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Điểm vào duy nhất để chạy việc parse: đi qua bulkhead khi đang bật,
 * hoặc chạy không giới hạn khi tắt (để so sánh trước/sau khi demo).
 */
@Component
public class CvParseExecutor {

    private final IsolatedTasks isolatedTasks;
    private final BulkheadMode mode;

    /** Khi KHÔNG có bulkhead: mỗi tin nhắn một thread, không giới hạn số lượng. */
    private final ThreadPoolExecutor unbounded;

    public CvParseExecutor(IsolatedTasks isolatedTasks, BulkheadMode mode) {
        this.isolatedTasks = isolatedTasks;
        this.mode = mode;
        AtomicInteger counter = new AtomicInteger();
        this.unbounded = (ThreadPoolExecutor) Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "parse-unbounded-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Parse CV hàng loạt (bất đồng bộ). Future hoàn thành khi parse xong. */
    public CompletableFuture<Void> submitBatch(Runnable task) {
        if (!mode.enabled()) {
            return CompletableFuture.runAsync(task, unbounded);
        }
        try {
            return isolatedTasks.runBatch(task);
        } catch (BulkheadFullException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    /** API đồng bộ của HR: chạy ngay trên thread của request. */
    public <T> T interactive(Supplier<T> task) {
        return mode.enabled() ? isolatedTasks.runInteractive(task) : task.get();
    }

    public int unboundedActive() {
        return unbounded.getActiveCount();
    }

    @PreDestroy
    void shutdown() {
        unbounded.shutdownNow();
    }
}
