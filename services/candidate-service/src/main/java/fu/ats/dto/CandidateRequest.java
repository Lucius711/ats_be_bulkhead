package fu.ats.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor@AllArgsConstructor
@ToString
public class CandidateRequest {
    @NotBlank
    private String fullName;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Email is not valid format")
    private String email;
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number is not valid format")
    private String phone;
    private String source;
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private List<UUID> skillIds;
}
