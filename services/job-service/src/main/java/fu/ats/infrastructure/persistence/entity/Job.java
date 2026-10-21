package fu.ats.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Job belongs to one Department.
     * <p>
     * jobs.department_id -> departments.id
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, referencedColumnName = "id")
    private Department department;

    /**
     * Reference to recruiter in Keycloak.
     * <p>
     * This is NOT a JPA relationship because recruiter
     * is managed by Keycloak / Identity service.
     */
    @Column(name = "recruiter_id")
    private Long recruiterId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String location;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "work_mode", length = 50)
    private String workMode;

    @Column(name = "salary_min", precision = 15, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 15, scale = 2)
    private BigDecimal salaryMax;

    @Column(length = 3)
    private String currency;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "utm_source", length = 150)
    private String utmSource;

    @Column(name = "utm_medium", length = 150)
    private String utmMedium;

    @Column
    private LocalDate deadline;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    /**
     * Skills are managed by another service, so only their ids are stored.
     */
    @ElementCollection
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill_id", nullable = false)
    @Builder.Default
    private List<UUID> skillIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

//    @Column(name = "created_by")
//    private Long createdBy;
//
//    @Column(name = "updated_by")
//    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

//    @Column(name = "created_at", nullable = false, updatable = false)
//    private OffsetDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private OffsetDateTime updatedAt;
//
//    @Column(name = "created_by")
//    private Long createdBy;
//
//    @Column(name = "updated_by")
//    private Long updatedBy;
//
//    @PrePersist
//    protected void onCreate() {
//        OffsetDateTime now = OffsetDateTime.now();
//        this.createdAt = now;
//        this.updatedAt = now;
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        this.updatedAt = OffsetDateTime.now();
//    }