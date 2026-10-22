package fu.ats.demo;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thông tin của 1 request, chụp lại TRÊN thread Tomcat lúc vừa nhận request.
 *
 * Được truyền TƯỜNG MINH qua tham số sang thread bulkhead -> không bị mất như ThreadLocal.
 * Đây cũng chính là cách đơn giản nhất để "mang context theo" khi đổi thread.
 *
 * @param id         mã request để lần theo trong log (lấy từ header X-Correlation-Id, không có thì tự sinh)
 * @param thread     thread Tomcat nhận request (http-nio-8083-exec-*)
 * @param receivedAt thời điểm nhận request (ms) -> tính được thời gian chờ trong hàng đợi bulkhead
 */
public record RequestInfo(String id, String thread, long receivedAt) {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static RequestInfo received(String correlationId) {
        String id = correlationId != null ? correlationId : "req-auto-" + COUNTER.incrementAndGet();
        return new RequestInfo(id, Thread.currentThread().getName(), System.currentTimeMillis());
    }
}
