package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import timesheets.dto.request.RegisterRequest;
import timesheets.dto.response.RegisterResponse;
import timesheets.enums.UserStatus;
import timesheets.repository.EmailVerificationTokenRepository;
import timesheets.repository.UserRepository;

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
      verify(userRepository, never())
          .save(any(User.class)); // check that the user was not saved again in the DB
    }
  }
}
