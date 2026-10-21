package fu.ats.infrastructure.persistence;

import fu.ats.infrastructure.persistence.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DepartmentJpaRepository extends JpaRepository<Department, UUID> {
}
