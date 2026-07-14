package timesheets.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.PasswordResetToken;

// this file helps with functions for the database, such that the functions interact with the
// database without writing SQL
// so what I did here is I just gave the decription of the functions, Spring Boot will be the one
// that creates the actual functions at runtime
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
  Optional<PasswordResetToken> findByToken(String token);
}
