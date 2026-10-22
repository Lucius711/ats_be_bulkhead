package fu.ats.client.cvparse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cv-parse-service",
        configuration = CvParseFeignConfig.class)
public interface CvParseClient {

    /** Parse CV - CHẬM (~8s). Không gọi trực tiếp, hãy đi qua CvParseGateway (có Bulkhead). */
    @GetMapping("/api/v1/cvparses/analyze/{cvId}")
    String analyze(@PathVariable("cvId") String cvId);
}
