package fu.ats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "job_applications")
public class Application{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false, columnDefinition = "uuid")
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "cv_id")
    private UUID cvId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cv_parse_status", length = 20)
    private CvParseStatus cvParseStatus;

    @Column(name = "department_id", columnDefinition = "uuid")
    private UUID departmentId;

    @Column(name = "transferred_from")
    private Long transferredFrom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_stage_id", nullable = false)
    private PipelineStage pipelineStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApplicationStatus status;

    @Column(name = "applied_at", nullable = false)
    private OffsetDateTime appliedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StageTransition> stageTransitions = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationNote> evaluationNotes = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interview> interviews = new ArrayList<>();

}
