package fu.ats.bulkhead;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Gia lap chuc nang phan tich CV (OCR / AI) chay cham.
 * Hai method lam cung mot viec, chi khac co / khong co Bulkhead de so sanh khi demo.
 */
@Slf4j
@Service
public class CvAnalysisService {

    @Value("${demo.cv-analysis.delay-ms:8000}")
    private long delayMs;

    // Semaphore Bulkhead: chay tren chinh thread Tomcat cua request, chi gioi han so luong dong thoi.
    @Bulkhead(name = "cvAnalysis", type = Bulkhead.Type.SEMAPHORE)
    public String analyzeWithBulkhead(String cvId) {
        return analyze(cvId);
    }

    public String analyzeWithoutBulkhead(String cvId) {
        return analyze(cvId);
    }

    private String analyze(String cvId) {
        String thread = Thread.currentThread().getName();
        log.info(">>> START phan tich {} tren {}", cvId, thread);
        try {
            Thread.sleep(delayMs); // gia lap OCR/AI cham, van giu thread trong luc cho
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bi ngat khi phan tich " + cvId, e);
        }
        log.info("<<< DONE  phan tich {} tren {}", cvId, thread);
        return "Skills extracted for " + cvId;
    }
}
