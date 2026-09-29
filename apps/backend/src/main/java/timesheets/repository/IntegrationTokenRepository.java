package timesheets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.IntegrationToken;

@Repository
public interface IntegrationTokenRepository extends JpaRepository<IntegrationToken, UUID> {
  Optional<IntegrationToken> findByWorkspaceMemberIdAndProvider(
      UUID workspaceMemberId, String provider);

  // google calendar will use this to support multiple providers
  List<IntegrationToken> findByWorkspaceMemberId(UUID workspaceMemberId);
}
