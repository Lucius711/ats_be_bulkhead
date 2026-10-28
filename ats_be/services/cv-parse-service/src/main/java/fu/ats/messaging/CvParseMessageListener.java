package fu.ats.messaging;

import fu.ats.bulkhead.CvParseExecutor;
import fu.ats.config.CvParseProperties;
import fu.ats.dto.CvAnalysisRequest;
import fu.ats.dto.CvAnalysisResponse;
import fu.ats.engine.AnalysisEngine;
import fu.ats.engine.Workload;
import fu.ats.service.CvParseService;
import fu.ats.shared.commonweb.messaging.rabbitmq.CvParseCommand;
import fu.ats.shared.commonweb.messaging.rabbitmq.CvParseResultEvent;
import fu.ats.shared.commonweb.messaging.rabbitmq.RabbitMessagingNames;
import fu.ats.storage.MinioCvFileReader;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CvParseMessageListener {

    private final CvParseService cvParseService;
    private final MinioCvFileReader fileReader;
    private final RabbitTemplate rabbitTemplate;
    private final CvParseExecutor cvParseExecutor;
    private final AnalysisEngine engine;
    private final CvParseProperties properties;

    /**
     * Nhận lệnh parse rồi giao cho ngăn bulkhead hàng loạt (ThreadPool), KHÔNG xử lý trên thread của listener.
     * Trả về CompletableFuture: container chỉ ack tin nhắn khi parse xong (ack mode MANUAL),
     * nên tin nhắn chưa xử lý vẫn an toàn trong RabbitMQ. Số tin nhắn nhận cùng lúc bị giới hạn bởi prefetch.
     */
    @RabbitListener(queues = RabbitMessagingNames.CV_PARSE_QUEUE)
    public CompletableFuture<Void> handle(CvParseCommand command) {
        log.info(
                "Received CV parse command messageId={}, cvId={}, filePath={}",
                command.messageId(),
                command.cvId(),
                command.filePath()
        );

        return cvParseExecutor.submitBatch(() -> process(command))
                .exceptionallyCompose(error -> requeueLaterIfBusy(command, error));
    }

    /**
     * Ngăn bulkhead đầy (chỉ xảy ra khi prefetch > sức chứa của ngăn): trả tin nhắn về RabbitMQ
     * sau 1 giây để xử lý lại, không đẩy sang dead-letter queue và không làm mất CV.
     */
    private CompletableFuture<Void> requeueLaterIfBusy(CvParseCommand command, Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (!(cause instanceof BulkheadFullException)) {
            return CompletableFuture.failedFuture(cause);
        }
        log.warn("CV parse bulkhead full, requeue later messageId={}, cvId={}", command.messageId(), command.cvId());
        return CompletableFuture.supplyAsync(() -> {
            throw new ImmediateRequeueAmqpException("CV parse bulkhead full");
        }, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
    }

    private void process(CvParseCommand command) {
        try {
            String cvText = fileReader.readText(command.filePath());
            String fileName = fileReader.fileName(command.filePath());

            // Engine phân tích là tài nguyên dùng chung với API đồng bộ của HR.
            CvAnalysisResponse analysis = engine.run(
                    Workload.BATCH,
                    properties.engine().parseDelay(),
                    () -> cvParseService.analyze(new CvAnalysisRequest(command.candidateId(), fileName, cvText))
            );

            publishResult(CvParseResultEvent.success(command, toParsedData(analysis)));

            log.info(
                    "CV parse completed messageId={}, cvId={}",
                    command.messageId(),
                    command.cvId()
            );
        } catch (Exception exception) {
            log.error(
                    "CV parse failed messageId={}, cvId={}, filePath={}",
                    command.messageId(),
                    command.cvId(),
                    command.filePath(),
                    exception
            );

            // Publish a failure result and acknowledge the command. This keeps
            // one bad file from being redelivered forever in the demo.
            publishResult(CvParseResultEvent.failure(
                    command,
                    "CV_PARSE_FAILED",
                    exception.getMessage()
            ));
        }
    }

    private void publishResult(CvParseResultEvent result) {
        rabbitTemplate.convertAndSend(
                RabbitMessagingNames.CV_EXCHANGE,
                RabbitMessagingNames.CV_RESULT_ROUTING_KEY,
                result
        );
    }

    private Map<String, Object> toParsedData(CvAnalysisResponse analysis) {
        Map<String, Object> parsedData = new LinkedHashMap<>();
        parsedData.put("candidateId", analysis.candidateId());
        parsedData.put("fileName", analysis.fileName());
        parsedData.put("email", analysis.email());
        parsedData.put("phone", analysis.phone());
        parsedData.put("skills", analysis.skills());
        parsedData.put("languages", analysis.languages());
        parsedData.put("education", analysis.education());
        parsedData.put("yearsOfExperience", analysis.yearsOfExperience());
        parsedData.put("summary", analysis.summary());
        parsedData.put("status", analysis.status());
        return parsedData;
    }
}
