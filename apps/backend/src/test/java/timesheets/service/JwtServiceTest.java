package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import timesheets.domain.User;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

  private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!";

  private JwtService jwtService;
  private User user;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    user = mock(User.class);
    when(user.getEmail()).thenReturn("john@momentum.co.za");
    when(user.getId()).thenReturn(userId);
  }

  @Nested
  @DisplayName("generateToken")
  class GenerateTokenTests {

    @Test
    @DisplayName("should embed email and user id")
    void embedsClaims() {
      String token = jwtService.generateToken(user, 1);

      assertThat(jwtService.extractEmail(token)).isEqualTo("john@momentum.co.za");
      assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("should expire in the future")
    void expiresInFuture() {
      String token = jwtService.generateToken(user, 1);

      assertThat(jwtService.getExpiration(token)).isAfter(new Date());
      assertThat(jwtService.isTokenExpired(token)).isFalse();
    }
  }

  @Nested
  @DisplayName("isTokenValid")
  class IsTokenValidTests {

    @Test
    @DisplayName("should be true for matching email")
    void trueForMatch() {
      String token = jwtService.generateToken(user, 1);

      assertThat(jwtService.isTokenValid(token, "john@momentum.co.za")).isTrue();
    }

    @Test
    @DisplayName("should be false for another email")
    void falseForMismatch() {
      String token = jwtService.generateToken(user, 1);

      assertThat(jwtService.isTokenValid(token, "other@momentum.co.za")).isFalse();
    }

    @Test
    @DisplayName("should throw for an expired token")
    void throwsWhenExpired() {
      String token = jwtService.generateToken(user, -1);

      assertThatThrownBy(() -> jwtService.isTokenValid(token, "john@momentum.co.za"))
          .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("should reject a tampered token")
    void rejectsTampered() {
      String token = jwtService.generateToken(user, 1);
      String tampered = token.substring(0, token.length() - 2) + "xx";

      assertThatThrownBy(() -> jwtService.extractEmail(tampered)).isInstanceOf(JwtException.class);
    }
  }

  @Nested
  @DisplayName("mfa challenge token")
  class MfaChallengeTests {

    @Test
    @DisplayName("should be recognised as an mfa challenge token")
    void recognisedAsChallenge() {
      String token = jwtService.generateMfaChallengeToken(user);

      assertThat(jwtService.isMfaChallengeToken(token)).isTrue();
      assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("should not treat a normal token as a challenge token")
    void normalTokenIsNotChallenge() {
      String token = jwtService.generateToken(user, 1);

      assertThat(jwtService.isMfaChallengeToken(token)).isFalse();
    }

    @Test
    @DisplayName("should return false for garbage instead of throwing")
    void garbageIsNotChallenge() {
      assertThat(jwtService.isMfaChallengeToken("garbage")).isFalse();
    }
  }
}
