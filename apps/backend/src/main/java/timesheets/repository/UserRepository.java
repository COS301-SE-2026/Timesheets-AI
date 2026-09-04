package timesheets.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timesheets.domain.User;

// Spring Data JPA repository for the User entity.
// JpaRepository gives us save(), findById(), findAll(), delete() etc for free.
// We only need to define custom queries here that aren't covered by the defaults.
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  // finds a user by their email address, used during login and registration checks
  Optional<User> findByEmail(String email);

  // checks if an email already exists without loading the full user object
  boolean existsByEmail(String email);

  // this finds the user id and loads their workspace, so that it loads user data with their
  // workspace
  @Query("SELECT u FROM User u " + "LEFT JOIN FETCH u.workspaceMembers " + "WHERE u.id = :userId")
  Optional<User> findByIdWithWorkspaces(@Param("userId") UUID userId);

  // this will update a user's paswsord hash
  @Modifying
  @Query(
      "UPDATE User u SET u.passwordHash = :passwordHash, u.updatedAt = :updatedAt WHERE u.id = :userId")
  void updatePassword(
      @Param("userId") UUID userId,
      @Param("passwordHash") String passwordHash,
      @Param("updatedAt") LocalDateTime updatedAt);

  // this will mark a users email as verified
  @Modifying
  @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
  void verifyEmail(@Param("userId") UUID userId);

  // this updates a users status, this is what will used for account activation and deactivation
  @Modifying
  @Query("UPDATE User u SET u.status = :status, u.updatedAt = :updatedAt WHERE u.id = :userId")
  void updateUserStatus(
      @Param("userId") UUID userId,
      @Param("status") String status,
      @Param("updatedAt") LocalDateTime updatedAt);

  // request account deletion
  @Modifying
  @Query(
      "UPDATE User u SET u.deletionRequestedAt = :requestedAt, u.deletionReason = :reason WHERE u.id = :userId")
  void requestDeletion(
      @Param("userId") UUID userId,
      @Param("requestedAt") LocalDateTime requestedAt,
      @Param("reason") String reason);

  // cancel deletion request
  @Modifying
  @Query(
      "UPDATE User u SET u.deletionRequestedAt = NULL, u.deletionReason = NULL WHERE u.id = :userId")
  void cancelDeletionRequest(@Param("userId") UUID userId);

  // mark deletion as processed
  @Modifying
  @Query("UPDATE User u SET u.deletionProcessedAt = :processedAt WHERE u.id = :userId")
  void markDeletionProcessed(
      @Param("userId") UUID userId, @Param("processedAt") LocalDateTime processedAt);

  // find users who requested deletion (for admin view)
  @Query("SELECT u FROM User u WHERE u.deletionRequestedAt IS NOT NULL AND u.status != 'SUSPENDED'")
  java.util.List<User> findPendingDeletions();

  // find users who requested deletion before a certain date
  @Query(
      "SELECT u FROM User u WHERE u.deletionRequestedAt IS NOT NULL AND u.deletionRequestedAt < :date")
  java.util.List<User> findDeletionRequestsBefore(@Param("date") LocalDateTime date);

  // count pending deletion requests
  @Query(
      "SELECT COUNT(u) FROM User u WHERE u.deletionRequestedAt IS NOT NULL AND u.status != 'SUSPENDED'")
  long countPendingDeletions();
}
