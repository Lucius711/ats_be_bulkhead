package fu.ats.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Small, explicit configuration object for the demo CV parser.
 */
@ConfigurationProperties(prefix = "cv-parse")
public record CvParseProperties(
        String mode,
        MinioProperties minio,
        @DefaultValue EngineProperties engine,
        @DefaultValue BatchProperties batch,
        @DefaultValue BulkheadProperties bulkhead
) {

    public record MinioProperties(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket
    ) {
    }

    /**
     * Engine phân tích CV (mô phỏng OCR/AI) có năng lực giới hạn: chỉ chạy được
     * {@code capacity} việc cùng lúc. Độ trễ mặc định 0 -> không ảnh hưởng khi chạy thật;
     * profile demo đặt parse-delay/match-delay để mô phỏng engine chậm.
     */
    public record EngineProperties(
            @DefaultValue("5") int capacity,
            @DefaultValue("0s") Duration parseDelay,
            @DefaultValue("0s") Duration matchDelay
    ) {
    }

    /**
     * Số tin nhắn parse tối đa mà cv-parse-service nhận từ RabbitMQ khi chưa ack.
     * Phần còn lại nằm chờ an toàn trong RabbitMQ (back-pressure).
     */
    public record BatchProperties(
            @DefaultValue("20") int prefetch
    ) {
    }

    /**
     * enabled: bulkhead đang bật hay tắt. toggleEnabled: cho phép bật/tắt lúc chạy qua API
     * (chỉ dùng khi demo, môi trường thật để false).
     */
    public record BulkheadProperties(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("false") boolean toggleEnabled
    ) {
    }
}
