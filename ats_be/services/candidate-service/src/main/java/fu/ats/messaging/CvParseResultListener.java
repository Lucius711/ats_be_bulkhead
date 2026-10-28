package fu.ats.messaging;

import fu.ats.entity.CvEntity;
import fu.ats.entity.ParseStatus;
import fu.ats.repository.CvEntityRepository;
import fu.ats.shared.commonweb.messaging.rabbitmq.CvParseResultEvent;
import fu.ats.shared.commonweb.messaging.rabbitmq.RabbitMessagingNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CvParseResultListener {

    private final CvEntityRepository cvEntityRepository;

    @RabbitListener(queues = RabbitMessagingNames.CV_RESULT_QUEUE)
    @Transactional
    public void handle(CvParseResultEvent event) {
        CvEntity cv = cvEntityRepository.findById(event.cvId())
                .orElseThrow(() -> new IllegalStateException("CV not found with id: " + event.cvId()));

        cv.setParseStatus(event.successful() ? ParseStatus.PARSED : ParseStatus.FAILED);
        cv.setParsedData(event.successful() ? event.parsedData() : null);
        cv.setParsedAt(OffsetDateTime.now());
        cvEntityRepository.save(cv);

        log.info(
                "Stored CV parse result messageId={}, cvId={}, successful={}",
                event.messageId(),
                event.cvId(),
                event.successful()
        );
    }
}
