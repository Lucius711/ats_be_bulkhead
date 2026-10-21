package fu.ats.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "candidate_skills")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSkills {

    @EmbeddedId
    private CandidateSkillId candidateSkillId;

    private LocalDate date;
}
