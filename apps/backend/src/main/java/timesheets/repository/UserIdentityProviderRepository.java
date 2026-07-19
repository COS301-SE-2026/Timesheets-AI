package timesheets.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.UserIdentityProvider;

@Repository
public interface UserIdentityProviderRepository extends JpaRepository<UserIdentityProvider, UUID> {
  Optional<UserIdentityProvider> findByProviderAndProviderUserId(
      String provider, String providerUserId);

  Optional<UserIdentityProvider> findByProviderAndUserId(String provider, UUID userId);
}
