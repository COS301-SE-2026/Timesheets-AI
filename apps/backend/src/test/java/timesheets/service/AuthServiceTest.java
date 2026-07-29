package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import timesheets.domain.EmailVerificationToken;
import timesheets.domain.User;
import timesheets.domain.UserIdentityProvider;
import timesheets.dto.request.AuthRequest;
import timesheets.dto.request.GoogleAuthRequest;
import timesheets.dto.request.RegisterRequest;
import timesheets.dto.response.AuthResponse;
import timesheets.dto.response.RegisterResponse;
import timesheets.enums.UserStatus;
import timesheets.repository.EmailVerificationTokenRepository;
import timesheets.repository.UserIdentityProviderRepository;
import timesheets.repository.UserMfaRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;

/*
-following the principle from the coding handbook of Arrange, Act, Assert
- unit tests for the AuthService class
*/

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = "google.client.id=test-google-client-id")
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

  // mocking all the the dependancies
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Mock private EmailService emailService;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;
  @Mock private UserMfaRepository userMfaRepository;
  @Mock private JwtService jwtService;
  @Mock private UserIdentityProviderRepository userIdentityProviderRepository;

  @InjectMocks private AuthService authService;

  private final UUID testUserId = UUID.randomUUID();
  private final String testEmail = "testEmail@momentum.co.za";
  private final String testPassword = "testPass123@";
  private final String testFirstName = "Test";
  private final String testLastName = "User";

  @BeforeEach
  void setUp() {}

  // ! helper functions
  // creating a user we will use to test
  private User createTestUser() {
    return User.builder()
        .id(testUserId)
        .email(testEmail)
        .firstName(testFirstName)
        .lastName(testLastName)
        .passwordHash("hashedPassword")
        .emailVerified(true)
        .status(UserStatus.ACTIVE)
        .failedLoginAttempts(0)
        .lockedUntil(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  // creating a registration request, and gives a request object
  private RegisterRequest createValidRegisterRequest() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail(testEmail);
    request.setPassword(testPassword);
    request.setFirstName(testFirstName);
    request.setLastName(testLastName);
    return request;
  }

  // creating an authorisation request with the test data
  private AuthRequest createValidAuthRequest() {
    AuthRequest request = new AuthRequest();
    request.setEmail(testEmail);
    request.setPassword(testPassword);
    return request;
  }

  @Nested // using this to group the related tests together
  @DisplayName("Register Tests")
  class RegisterTests {

    @Test
    @DisplayName("user should register properly")
    void registerNewUser() {

      // ARRANGE: setting up the mock user and stuff
      RegisterRequest request = createValidRegisterRequest();
      User savedUser = createTestUser(); // creating the valid user to be returned

      // repo return empty if the user exists
      when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
      when(passwordEncoder.encode(testPassword)).thenReturn("hashedPassword");
      when(userRepository.save(any(User.class))).thenReturn(savedUser);
      when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
          .thenReturn(EmailVerificationToken.builder().build());

      // ACT: testing the register method
      RegisterResponse response = authService.register(request);

      // ASSERT: checking that the response gives me the data I expect
      assertThat(response).isNotNull();
      assertThat(response.getEmail()).isEqualTo(testEmail);
      assertThat(response.getFirstName()).isEqualTo(testFirstName);
      assertThat(response.getLastName()).isEqualTo(testLastName);
      assertThat(response.getMessage()).contains("Verification email sent");

      verify(userRepository).save(any(User.class)); // did a user get saved?
      // making sure verification email was sent
      verify(emailService).sendVerificationEmail(eq(testEmail), eq(testFirstName), anyString());
    }

    @Test
    @DisplayName("the verification email should be resent for unverified email")
    void resendEmailVerification() {

      // ARRANGE
      RegisterRequest request = createValidRegisterRequest();
      User existingUser = createTestUser();
      existingUser.setEmailVerified(false);

      // the user already exists in DB but is unverified
      when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));
      when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
          .thenReturn(EmailVerificationToken.builder().build());

      // ACT: test the register method
      RegisterResponse response = authService.register(request);

      // ASSERT
      assertThat(response.getMessage()).contains("Verification email sent");

      // checking that the new verification email was sent to the user
      verify(emailVerificationTokenRepository).deleteByUserId(existingUser.getId());
      verify(emailService).sendVerificationEmail(eq(testEmail), eq(testFirstName), anyString());

      // check that the user was not saved again in the DB
      verify(userRepository, never()).save(any(User.class));
    }
  }

  @Test
  @DisplayName("reject registration for verified user")
  void shouldRejectRegistrationForAlreadyVerifiedUser() {

    // ARRANGE: set the user
    RegisterRequest request = createValidRegisterRequest();
    User existingUser = createTestUser();

    existingUser.setEmailVerified(true);

    when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));

    // ACT and ASSERT: an exception should be thrown
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email already exists");
  }

  @Nested
  @DisplayName("Login Tests")
  class LoginTests {

    @Test
    @DisplayName("logs in properly with correct credentials")
    void loginWithValidDetails() {
      // ARRANGE: setting up user
      AuthRequest request = createValidAuthRequest();
      User user = createTestUser();

      when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

      // the passwords should match
      when(passwordEncoder.matches(testPassword, user.getPasswordHash())).thenReturn(true);
      when(workspaceMemberRepository.findByUserId(testUserId))
          .thenReturn(List.of()); // no workspaces yet
      when(userMfaRepository.findByUserId(testUserId)).thenReturn(Optional.empty()); // no mfa yet

      // mock token generated
      when(jwtService.generateToken(any(User.class), eq(1))).thenReturn("jwt-token");

      // ACT: test login function
      AuthResponse response = authService.login(request);

      // ASSERT: checks that the data I expect is what I am getting
      assertThat(response).isNotNull();
      assertThat(response.getToken()).isEqualTo("jwt-token");
      assertThat(response.getUser()).isNotNull();
      assertThat(response.getUser().getEmail()).isEqualTo(testEmail);
      assertThat(response.getRequiresMfa()).isFalse();

      verify(userRepository).save(user);
      assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
      assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("reject login with invalid email")
    void rejectInvalidEmail() {

      // ARRANGE: set up with an invalid email, cause I did not set up that user with email
      AuthRequest request = createValidAuthRequest();

      when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

      // ACT and ASSERT
      assertThatThrownBy(() -> authService.login(request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("invalid credentials");
    }
  }

  @Nested
  @DisplayName("Google Auth Tests")
  class GoogleAuthTests {

    @Test
    @DisplayName("existing user should login with google")
    void loginWithGoogle() {

      // ARRANGE
      GoogleAuthRequest request = new GoogleAuthRequest();
      request.setIdToken("swagger-test");

      UserIdentityProvider identityProvider =
          UserIdentityProvider.builder()
              .userId(testUserId)
              .provider("GOOGLE")
              .providerUserId("google-test-user-123")
              .build();
      User user = createTestUser();

      when(userIdentityProviderRepository.findByProviderAndProviderUserId(
              "GOOGLE", "google-test-user-123"))
          .thenReturn(Optional.of(identityProvider));
      when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
      when(workspaceMemberRepository.findByUserId(testUserId)).thenReturn(List.of());
      when(userMfaRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
      when(jwtService.generateToken(any(User.class), eq(1))).thenReturn("jwt-token");

      // ACT: testing that method
      AuthResponse response = authService.googleAuth(request);

      // ASSERT
      assertThat(response).isNotNull();
      assertThat(response.getToken()).isEqualTo("jwt-token"); // simulating a returned jwt token
      assertThat(response.getUser().getEmail()).isEqualTo(testEmail);
    }
  }
}
