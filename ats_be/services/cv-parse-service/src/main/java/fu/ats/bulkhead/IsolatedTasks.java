package fu.ats.bulkhead;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Hai ngăn bulkhead. Tách thành bean riêng vì @Bulkhead chạy qua Spring AOP proxy:
 * gọi nội bộ this.xxx() sẽ bỏ qua annotation.
 */
@Component
public class IsolatedTasks {

    /**
     * Ngăn cho parse CV hàng loạt: chạy trên pool riêng "bulkhead-cvParseBatch-*"
     * (max-thread-pool-size thread + queue-capacity chỗ chờ). Bắt buộc trả CompletableFuture.
     */
    @Bulkhead(name = BulkheadNames.BATCH, type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Void> runBatch(Runnable task) {
        task.run();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Ngăn cho API đồng bộ của HR: chạy ngay trên thread của request,
     * tối đa max-concurrent-calls việc cùng lúc; đầy -> BulkheadFullException (503).
     */
    @Bulkhead(name = BulkheadNames.INTERACTIVE, type = Bulkhead.Type.SEMAPHORE)
    public <T> T runInteractive(Supplier<T> task) {
        return task.get();
    }
}
