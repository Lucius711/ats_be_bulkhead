package fu.ats.dto;

import java.util.List;
import java.util.UUID;

public record CvAnalysisResponse(
        UUID candidateId,
        String fileName,
        String email,
        String phone,
        List<String> skills,
        List<String> languages,
        String education,
        int yearsOfExperience,
        String summary,
        String status
) {
}
