package fu.ats.repository;

import fu.ats.entity.CandidateSkillId;
import fu.ats.entity.CandidateSkills;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateSkillsRepository extends JpaRepository<CandidateSkills, CandidateSkillId> {
}
