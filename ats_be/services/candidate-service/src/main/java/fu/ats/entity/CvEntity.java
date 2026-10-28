package fu.ats.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@Entity
@Table(name = "cvs")
public class CvEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidates candidate;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    // MIME types for DOCX/PDF files can be longer than 50 characters.
    @Column(name = "file_type", length = 255)
    private String fileType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "object_key", length = 1000, nullable = false, unique = true)
    private String objectKey;

    /**
     * Raw output written back by cv-parser-service after it parses this file
     * (candidate-service does not parse CVs itself — it only persists the
     * result, most likely via an async event/callback from that service).
     * As opposed to CandidateEntity.parsedCvData, which holds the
     * curated/normalized VO derived from this raw payload.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_data", columnDefinition = "jsonb")
    private Map<String, Object> parsedData;

    /**
     * Set by candidate-service on upload (PENDING), then updated to
     * PARSED/FAILED once cv-parser-service reports back the parse result.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", length = 50)
    private ParseStatus parseStatus;

    @Column(name = "uploaded_at", columnDefinition = "timestamptz")
    private OffsetDateTime uploadedAt;

    /** Thời điểm nhận kết quả parse từ cv-parse-service (PARSED hoặc FAILED). */
    @Column(name = "parsed_at", columnDefinition = "timestamptz")
    private OffsetDateTime parsedAt;
}
