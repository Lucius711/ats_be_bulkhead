package fu.ats.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CvAnalysisRequest(
        UUID candidateId,
        String fileName,
        @NotBlank(message = "cvText must not be blank")
        String cvText
) {
}
