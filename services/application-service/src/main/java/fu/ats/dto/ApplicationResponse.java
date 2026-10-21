package fu.ats.dto;

import fu.ats.entity.ApplicationStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ApplicationResponse {

    private UUID id;

    private UUID jobId;

    private UUID candidateId;

    private CandidateView candidate;

    private UUID cvId;

    private UUID departmentId;

    private Long transferredFrom;

    private Long pipelineStageId;

    private ApplicationStatus status;

    private OffsetDateTime appliedAt;
}

