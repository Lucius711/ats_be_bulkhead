package fu.ats.bulkhead;

import fu.ats.config.CvParseProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bulkhead đang bật hay tắt. Mặc định bật; chỉ được đổi lúc chạy khi
 * cv-parse.bulkhead.toggle-enabled=true (dùng để demo so sánh trước/sau).
 */
@Component
public class BulkheadMode {

    private final AtomicBoolean enabled;
    private final boolean toggleEnabled;

    public BulkheadMode(CvParseProperties properties) {
        this.enabled = new AtomicBoolean(properties.bulkhead().enabled());
        this.toggleEnabled = properties.bulkhead().toggleEnabled();
    }

    public boolean enabled() {
        return enabled.get();
    }

    public boolean toggleEnabled() {
        return toggleEnabled;
    }

    public void set(boolean value) {
        if (!toggleEnabled) {
            throw new IllegalStateException("Bulkhead toggle is disabled (cv-parse.bulkhead.toggle-enabled=false)");
        }
        enabled.set(value);
    }
}
