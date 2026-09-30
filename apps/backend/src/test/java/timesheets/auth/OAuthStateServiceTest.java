package timesheets.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("OAuthStateService Unit Tests")
class OAuthStateServiceTest {

  private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!";

  private OAuthStateService service;

  @BeforeEach
  void setUp() {
    service = new OAuthStateService();
    ReflectionTestUtils.setField(service, "secret", SECRET);
  }

  @Nested
  @DisplayName("generateState")
  class GenerateStateTests {

    @Test
    @DisplayName("should produce a signed three part token")
    void producesJwt() {
      String state = service.generateState(UUID.randomUUID(), "GITHUB");

      assertThat(state.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("should produce different states for different members")
    void differsPerMember() {
      String a = service.generateState(UUID.randomUUID(), "GITHUB", "/settings");
      String b = service.generateState(UUID.randomUUID(), "GITHUB", "/settings");

      assertThat(a).isNotEqualTo(b);
    }
  }

  @Nested
  @DisplayName("validateState")
  class ValidateStateTests {

    @Test
    @DisplayName("should accept a state it generated")
    void acceptsOwnState() {
      String state = service.generateState(UUID.randomUUID(), "JIRA", "/settings");

      assertThat(service.validateState(state)).isNotNull();
    }

    @Test
    @DisplayName("should accept a state without a return path")
    void acceptsStateWithoutReturnPath() {
      String state = service.generateState(UUID.randomUUID(), "JIRA");

      assertThat(service.validateState(state)).isNotNull();
    }

    @Test
    @DisplayName("should reject a tampered state")
    void rejectsTampered() {
      String state = service.generateState(UUID.randomUUID(), "GITHUB");
      String tampered = state.substring(0, state.length() - 2) + "xx";

      assertThatThrownBy(() -> service.validateState(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("should reject a state signed with another secret")
    void rejectsOtherSecret() {
      OAuthStateService other = new OAuthStateService();
      ReflectionTestUtils.setField(other, "secret", "another-secret-key-that-is-32-bytes-long!!");
      String foreign = other.generateState(UUID.randomUUID(), "GITHUB");

      assertThatThrownBy(() -> service.validateState(foreign)).isInstanceOf(JwtException.class);
    }
  }
}
