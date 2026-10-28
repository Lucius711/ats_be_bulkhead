package fu.ats.repository;

import fu.ats.entity.CvEntity;
import fu.ats.entity.ParseStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CvEntityRepository extends JpaRepository<CvEntity, Long> {

    List<CvEntity> findByCandidateId(UUID candidateId);

    @EntityGraph(attributePaths = "candidate")
    List<CvEntity> findAllByOrderByUploadedAtDesc(Pageable pageable);

    long countByParseStatus(ParseStatus parseStatus);
}
