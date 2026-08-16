package timesheets.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.IntegrationToken;

@Repository
public interface IntegrationTokenRepository extends JpaRepository<IntegrationToken, UUID> {
  Optional<IntegrationToken> findByWorkspaceMemberIdAndProvider(
      UUID workspaceMemeberId, String provider);
}
