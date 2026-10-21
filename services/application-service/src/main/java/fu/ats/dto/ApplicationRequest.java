package fu.ats.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ApplicationRequest {

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Job ID is required")
    private UUID jobId;

    @NotNull(message = "CV URL is required")
    private String cvUrl;
}

