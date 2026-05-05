package guard.ics.backend.rbac.repository;

import guard.ics.backend.rbac.entity.PermissionTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionTypeRepository extends JpaRepository<PermissionTypeEntity, Long> {
    Optional<PermissionTypeEntity> findByName(String name);
    boolean existsByName(String name);
}
