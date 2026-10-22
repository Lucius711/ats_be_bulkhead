package fu.ats.repository;

import fu.ats.entity.Application;
import fu.ats.entity.CvParseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {


    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    // Cập nhật kết quả parse CV từ thread của bulkhead (ngoài transaction nộp hồ sơ)
    @Modifying
    @Transactional
    @Query("update Application a set a.cvParseStatus = :status where a.id = :id")
    int updateCvParseStatus(@Param("id") UUID id, @Param("status") CvParseStatus status);
}

