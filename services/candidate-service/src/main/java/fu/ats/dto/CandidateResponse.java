package fu.ats.dto;

import fu.ats.entity.CandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CandidateResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private CandidateStatus status;
    private Boolean isDuplicate;
}
