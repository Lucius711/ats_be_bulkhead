package fu.ats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pipeline_stages")
public class PipelineStage{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(length = 30)
    private String color;

    @Column(name = "is_default", nullable = false)
    private boolean defaultStage;

}
