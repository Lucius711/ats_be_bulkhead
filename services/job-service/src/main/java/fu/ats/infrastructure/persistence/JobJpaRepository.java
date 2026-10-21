package fu.ats.infrastructure.persistence;

import fu.ats.infrastructure.persistence.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobJpaRepository extends JpaRepository<Job, UUID>{
}
