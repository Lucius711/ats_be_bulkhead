package fu.ats.repository;

import fu.ats.entity.Skills;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skills, UUID> {
}
