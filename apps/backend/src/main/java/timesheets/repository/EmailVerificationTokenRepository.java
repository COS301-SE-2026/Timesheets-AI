package timesheets.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timesheets.domain.EmailVerificationToken;

// this file helps with functions for the database, such that the functions interact with the
// database without writing SQL
// so what I did here is I just gave the decription of the functions, Spring Boot will be the one
// that creates the actual functions at runtime
@Repository
public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, UUID> {
  Optional<EmailVerificationToken> findByToken(String token);

  void deleteByUserId(UUID userId);

  // mark token as verified
  @Modifying
  @Query(
      "UPDATE EmailVerificationToken evt SET evt.verifiedAt = :verifiedAt WHERE evt.token = :token")
  void markAsVerified(@Param("token") String token, @Param("verifiedAt") LocalDateTime verifiedAt);

  // delete expired tokens
  @Modifying
  @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :now")
  void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
