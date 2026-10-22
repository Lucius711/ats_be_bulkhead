package fu.ats.demo;

/**
 * Dấu vết của 1 lời gọi parse CV sang cv-parse-service, để khi demo nhìn response là thấy ngay sự khác biệt.
 *
 * @param mode          NONE | THREAD_POOL
 * @param requestId     mã request, truyền qua tham số nên thread nào cũng đọc được
 * @param requestThread thread Tomcat nhận request (http-nio-8083-exec-*)
 * @param executedOn    thread thực sự chạy lời gọi Feign
 *                      - NONE       : trùng requestThread
 *                      - THREAD_POOL: bulkhead-cvParse-*
 * @param correlationId header X-Correlation-Id đọc TỪ ThreadLocal tại thread chạy Feign
 *                      (THREAD_POOL không còn request context -> mất header)
 * @param result        kết quả parse trả về từ cv-parse-service
 * @param queueMs       thời gian từ lúc nhận request tới lúc bắt đầu gọi Feign (= thời gian chờ trong hàng đợi bulkhead)
 * @param callMs        thời gian lời gọi Feign (parse CV)
 */
public record CallTrace(
        String mode,
        String requestId,
        String requestThread,
        String executedOn,
        String correlationId,
        String result,
        long queueMs,
        long callMs) {
}
