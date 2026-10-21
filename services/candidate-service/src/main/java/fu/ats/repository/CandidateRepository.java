package fu.ats.repository;

import fu.ats.entity.Candidates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface CandidateRepository extends JpaRepository<Candidates, UUID> {
}
