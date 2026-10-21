package fu.ats.client.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;


@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallbackFactory.class);

    @Override
    public NotificationClient create(Throwable cause) {
        return () -> {
            log.warn("notification-service failed, storing event for later. cause={}", cause.toString());
//            outboxRepository.save(OutboxMessage.from(event));   // retried later by a scheduled job
        };
    }
}