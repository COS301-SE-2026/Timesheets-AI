package timesheets.service;

// integration tests for the JwtService to verify that it correctly generates JWT tokens with the
// expected claims and structure
// extracts the email claim from tokens, validates tokens against user details, checks for token
// expiration
// and retrieves the expiration date from tokens
// ensuring that the service behaves as expected in various scenarios when integrated into the
// application's authentication and authorization processes

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import timesheets.domain.User;
import timesheets.enums.UserStatus;

class JwtServiceTest {

  private JwtService jwtService;
  private User user;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(
        jwtService, "secret", "test-secret-key-that-is-long-enough-for-hmac-sha384-signing");
    // sets a test secret key for signing the JWT tokens, ensuring that the JwtService can generate
    // and validate tokens during the tests without relying on external configuration

    user =
        User.builder() // builds sample user with necessary fields for token generation and
            // validation
            .id(UUID.randomUUID())
            .email("bob@momentum.co.za")
            .firstName("Bob")
            .lastName("Dev")
            .passwordHash("hash")
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .loginAttempts(0)
            .build();
  }

  @Test
  void generateToken_returnsNonNullToken() {
    // this test checks the generateToken method to ensure that it returns a non-null
    // and non-blank JWT token string when provided with a valid user object and expiration time
    // verifying that the token generation process is functioning correctly and producing usable
    // tokens for authentication purposes
    String token = jwtService.generateToken(user, 1);
    assertThat(token).isNotNull().isNotBlank();
  }

  @Test
  void extractEmail_returnsCorrectEmail() {
    // this test checks the extractEmail method to ensure that it correctly extracts the email claim
    // from a valid JWT token
    // verifying that the token contains the expected user information
    String token = jwtService.generateToken(user, 1);
    assertThat(jwtService.extractEmail(token)).isEqualTo("bob@momentum.co.za");
  }

  @Test
  void isTokenValid_returnsTrueForValidToken() {
    // this test checks the isTokenValid method to ensure that it returns true for a valid JWT token
    // that is correctly signed
    // not expired, and matches the expected email
    String token = jwtService.generateToken(user, 1);
    assertThat(jwtService.isTokenValid(token, "bob@momentum.co.za")).isTrue();
  }

  @Test
  void isTokenValid_returnsFalseForWrongEmail() {
    // this test checks the isTokenValid method to ensure that it returns false for a valid JWT
    // token that is correctly signed
    // not expired, but does not match the expected email, verifying that the method correctly
    // validates
    // the token against the provided email and does not allow tokens to be considered valid if the
    // email claim does not match
    String token = jwtService.generateToken(user, 1);
    assertThat(jwtService.isTokenValid(token, "alice@momentum.co.za")).isFalse();
  }

  @Test
  void isTokenExpired_returnsFalseForFreshToken() {
    // this test checks the isTokenExpired method to ensure that it returns false for a freshly
    // generated JWT token that has not yet expired
    // verifying that the method correctly identifies valid tokens as not expired, allowing them to
    // be
    // used for authentication and access to protected resources until they reach their expiration
    // time
    String token = jwtService.generateToken(user, 1);
    assertThat(jwtService.isTokenExpired(token)).isFalse();
  }

  @Test
  void getExpiration_returnsNonNullDate() {
    // this test checks the getExpiration method to ensure that it returns a non-null expiration
    // date for a valid JWT token
    // verifying that the method can successfully extract the expiration claim from the token and
    // provide it
    // in a usable format for checking token validity and managing token lifecycles within the
    // application
    String token = jwtService.generateToken(user, 1);
    assertThat(jwtService.getExpiration(token)).isNotNull();
  }
}
