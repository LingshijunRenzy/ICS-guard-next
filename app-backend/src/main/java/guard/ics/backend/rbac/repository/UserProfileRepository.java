package guard.ics.backend.rbac.repository;

import guard.ics.backend.rbac.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
}
