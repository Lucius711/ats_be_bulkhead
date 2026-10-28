package fu.ats.repository;

import fu.ats.entity.CvEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CvEntityRepository extends JpaRepository<CvEntity, Long> {

    List<CvEntity> findByCandidateId(UUID candidateId);
}
