@Repository
public interface IntegrationTokenRepository extends JpaRepository<IntegrationToken, UUID> {
  optional<IntegrationToken> findByWorkspaceMemberIdAndProvider(
      UUID workspaceMemeberId, String provider);
}
