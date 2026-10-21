package fu.ats.bulkhead;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cvparses")
@RequiredArgsConstructor
public class CvAnalysisController {

    private final CvAnalysisService cvAnalysisService;

    /** Co Bulkhead: toi da 3 CV cung luc, vuot -> 503 ngay. */
    @GetMapping("/analyze/{cvId}")
    public Map<String, String> analyze(@PathVariable String cvId) {
        return Map.of("cvId", cvId,
                "result", cvAnalysisService.analyzeWithBulkhead(cvId),
                "thread", Thread.currentThread().getName());
    }

    /** Khong Bulkhead: de thay request phan tich CV chiem het thread Tomcat. */
    @GetMapping("/analyze-no-bulkhead/{cvId}")
    public Map<String, String> analyzeNoBulkhead(@PathVariable String cvId) {
        return Map.of("cvId", cvId,
                "result", cvAnalysisService.analyzeWithoutBulkhead(cvId),
                "thread", Thread.currentThread().getName());
    }

    /** Chuc nang nhe khong lien quan (gia lap HR xem danh sach ung vien) - phai luon nhanh. */
    @GetMapping("/quick-list")
    public List<String> quickList() {
        return List.of("Nguyen Van A", "Tran Thi B", "Le Van C");
    }
}
