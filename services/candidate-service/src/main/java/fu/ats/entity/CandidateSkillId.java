package fu.ats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CandidateSkillId implements Serializable {
    @ManyToOne
    @JoinColumn(name = "candidate_id", referencedColumnName ="id" )
    private Candidates id;

    @ManyToOne
    @JoinColumn(name = "skill_id", referencedColumnName = "id")
    private Skills skillId;

}
