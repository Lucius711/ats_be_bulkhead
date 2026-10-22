package fu.ats.service;

import fu.ats.client.cvparse.CvParseGateway;
import fu.ats.entity.CvParseStatus;
import fu.ats.exception.DownstreamUnavailableException;
import fu.ats.exception.ServiceBusyException;
import fu.ats.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Gửi CV đi parse NỀN qua CvParseGateway (ThreadPool Bulkhead) - nộp hồ sơ KHÔNG chờ parse.
 *
 *  - Hồ sơ được lưu với cvParseStatus = PENDING, rồi mới gửi parse SAU KHI transaction commit
 *    (tránh thread bulkhead cập nhật 1 hồ sơ chưa tồn tại trong DB).
 *  - Parse xong                           -> PARSED
 *  - Bulkhead đầy / cv-parse-service bận  -> giữ PENDING (cô lập: nộp hồ sơ vẫn thành công, parse lại sau)
 *  - Lỗi khác                             -> FAILED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CvParseDispatcher {

    private final CvParseGateway cvParseGateway;
    private final ApplicationRepository applicationRepository;

    public void dispatchAfterCommit(UUID applicationId, UUID cvId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(applicationId, cvId);
                }
            });
        } else {
            dispatch(applicationId, cvId);
        }
    }

    private void dispatch(UUID applicationId, UUID cvId) {
        cvParseGateway.parse(cvId.toString()).whenComplete((result, error) -> {
            if (error == null) {
                applicationRepository.updateCvParseStatus(applicationId, CvParseStatus.PARSED);
                log.info("[cv-parse] application {} PARSED | {}", applicationId, result);
                return;
            }
            RuntimeException cause = ServiceBusyException.fromBulkhead("cv-parse-service", error);
            if (cause instanceof DownstreamUnavailableException || cause instanceof ServiceBusyException) {
                log.warn("[cv-parse] application {} giu PENDING (cv-parse ban/qua tai) -> parse lai sau | {}",
                        applicationId, cause.getMessage());
            } else {
                applicationRepository.updateCvParseStatus(applicationId, CvParseStatus.FAILED);
                log.error("[cv-parse] application {} FAILED", applicationId, cause);
            }
        });
    }
}
