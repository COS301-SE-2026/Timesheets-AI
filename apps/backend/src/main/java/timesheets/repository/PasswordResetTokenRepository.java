package timesheets.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timesheets.domain.PasswordResetToken;

// this file helps with functions for the database, such that the functions interact with the
// database without writing SQL
// so what I did here is I just gave the decription of the functions, Spring Boot will be the one
// that creates the actual functions at runtime
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
  Optional<PasswordResetToken> findByToken(String token);

  // mark token as used
  @Modifying
  @Query("UPDATE PasswordResetToken prt SET prt.usedAt = :usedAt WHERE prt.token = :token")
  void markAsUsed(@Param("token") String token, @Param("usedAt") LocalDateTime usedAt);

  // delete expired tokens
  @Modifying
  @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
  void deleteExpiredTokens(@Param("now") LocalDateTime now);

  // delete tokens by user ID
  @Modifying
  @Query("DELETE FROM PasswordResetToken prt WHERE prt.userId = :userId")
  void deleteByUserId(@Param("userId") UUID userId);
}
