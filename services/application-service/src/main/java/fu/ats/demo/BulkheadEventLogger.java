package fu.ats.demo;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In ra console mỗi khi bulkhead NHẬN / TỪ CHỐI một lời gọi, kèm số thread và hàng đợi đang dùng.
 * Khi demo chỉ cần nhìn log là thấy bulkhead đang làm gì.
 */
@Configuration
@Slf4j
public class BulkheadEventLogger {

    @Bean
    ApplicationRunner registerBulkheadEventLogging(ThreadPoolBulkheadRegistry threadPoolRegistry) {
        return args -> {
            // Lấy đúng instance mà @Bulkhead sử dụng (cùng registry, cùng tên)
            ThreadPoolBulkhead threadPool = threadPoolRegistry.bulkhead(BulkheadDemoService.THREAD_POOL_BULKHEAD);
            threadPool.getEventPublisher()
                    .onCallPermitted(e -> log.info("[THREADPOOL] ACCEPTED   | threads {}/{} | queue {}/{}",
                            threadPool.getMetrics().getThreadPoolSize(),
                            threadPool.getMetrics().getMaximumThreadPoolSize(),
                            threadPool.getMetrics().getQueueDepth(),
                            threadPool.getMetrics().getQueueCapacity()))
                    .onCallRejected(e -> log.warn("[THREADPOOL] REJECTED   | pool + queue day"));
        };
    }
}
