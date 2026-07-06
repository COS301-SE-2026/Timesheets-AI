package timesheets.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.UserMfa;

@Repository
public interface UserMfaRepository extends JpaRepository<UserMfa, UUID> {
  Optional<UserMfa> findByUserId(UUID userId);
}
