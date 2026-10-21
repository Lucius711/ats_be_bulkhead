package fu.ats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table(name = "candidates")
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Candidates {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "auth_provider", length = 50)
    private String authProvider;

    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private CandidateStatus status;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "source", length = 150)
    private String source;

    @Column(name = "utm_source", length = 150)
    private String utmSource;

    @Column(name = "utm_medium", length = 150)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 255)
    private String utmCampaign;

    @Column(name = "is_duplicate")
    private Boolean isDuplicate;

}
