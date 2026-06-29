package timesheets.service;

// these are integration tests for the AuthService, which handles user authentication and related
// operations such as
// registration, email verification, login, logout, and password reset in the timesheets application
// The tests use Mockito to mock the dependencies of the AuthService, allowing us to isolate the
// service logic and verify its behavior under various conditions, such as valid inputs, error
// scenarios, and edge
// cases. Each test method corresponds to a specific functionality of the AuthService, ensuring that
// the service behaves as expected in different scenarios and that it correctly interacts with its
// dependencies
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import timesheets.domain.*;
import timesheets.dto.request.*;
import timesheets.dto.response.*;
import timesheets.enums.UserStatus;
import timesheets.repository.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock UserRepository userRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock JwtService jwtService;
  @Mock EmailService emailService;
  @Mock TokenBlacklistService tokenBlacklistService;
  @Mock PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;

  @InjectMocks AuthService authService;

  private User activeUser;

  @BeforeEach
  void setUp() {
    activeUser =
        User.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .email("bob@momentum.co.za")
            .firstName("Bob")
            .lastName("Dev")
            .passwordHash("$2a$10$hashedpassword")
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .loginAttempts(0)
            .build();
  }

  // register
  @Test
  void register_returnsResponseWithEmailAndMessage() {
    // this test checks that when a valid registration request is made with a new email,
    // the AuthService's register method returns a RegisterResponse containing the registered email
    // and a message indicating that a verification email has been sent.
    // it also verifies that the emailService's sendVerificationEmail method is called with the
    // correct parameters to send the verification email to the new user.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("newuser@momentum.co.za");
    request.setFirstName("New");
    request.setLastName("User");
    request.setPassword("Password123!");

    when(userRepository.existsByEmail("newuser@momentum.co.za")).thenReturn(false);
    when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
    when(userRepository.save(any()))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              u =
                  User.builder()
                      .id(UUID.randomUUID())
                      .email(u.getEmail())
                      .firstName(u.getFirstName())
                      .lastName(u.getLastName())
                      .passwordHash(u.getPasswordHash())
                      .emailVerified(false)
                      .loginAttempts(0)
                      .status(UserStatus.ACTIVE)
                      .createdAt(LocalDateTime.now())
                      .build();
              return u;
            });
    when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    RegisterResponse response = authService.register(request);

    assertThat(response.getEmail()).isEqualTo("newuser@momentum.co.za");
    assertThat(response.getMessage()).contains("Verification email sent");
    verify(emailService)
        .sendVerificationEmail(eq("newuser@momentum.co.za"), eq("New"), anyString());
  }

  @Test
  void register_throwsWhenEmailDomainNotAccepted() {
    // this test checks that when a resgistration request is made with an email that has a domain
    // not accepted by the application (
    // e.g. or @momentum.co.za), the AuthService's register method throws an
    // IllegalArgumentException with a message indicating that the email domain is not accepted.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("user@gmail.com");
    request.setFirstName("X");
    request.setLastName("Y");
    request.setPassword("Password123!");

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email domain not accepted");
  }

  @Test
  void register_throwsWhenEmailAlreadyExists() {
    // this test checks that when a registration request is made with an email that already exists
    // in the system,
    // the AuthService's register method throws an IllegalArgumentException with a message
    // indicating that the email already exists.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("bob@momentum.co.za");
    request.setFirstName("Bob");
    request.setLastName("Dev");
    request.setPassword("Password123!");

    when(userRepository.existsByEmail("bob@momentum.co.za")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email already exists");
  }

  @Test
  void register_acceptsMomentumCozaDomain() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("user@momentum.co.za");
    request.setFirstName("X");
    request.setLastName("Y");
    request.setPassword("Password123!");

    when(userRepository.existsByEmail("user@momentum.co.za")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("hashed");
    when(userRepository.save(any()))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              return User.builder()
                  .id(UUID.randomUUID())
                  .email(u.getEmail())
                  .firstName(u.getFirstName())
                  .lastName(u.getLastName())
                  .passwordHash(u.getPasswordHash())
                  .emailVerified(false)
                  .loginAttempts(0)
                  .status(UserStatus.ACTIVE)
                  .createdAt(LocalDateTime.now())
                  .build();
            });
    when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThatNoException().isThrownBy(() -> authService.register(request));
  }

  // verifyEmail

  @Test
  void verifyEmail_successfullyVerifiesToken() {
    // `-this test checks that when a valid email verification token is provided to the
    // AuthService's verifyEmail method,
    // the method successfully verifies the token, updates the user's emailVerified status to true,
    // and returns a MessageResponse indicating that the email has been verified
    // it also verifies that the emailVerificationTokenRepository's save method is called with a
    // token that has its verified property set to true, ensuring that the token is marked as used
    // after successful verification.
    String tokenStr = UUID.randomUUID().toString();
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .token(tokenStr)
            .userId(activeUser.getId())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .verified(false)
            .build();

    when(emailVerificationTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));
    when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
    when(emailVerificationTokenRepository.save(any())).thenReturn(token);
    when(userRepository.save(any())).thenReturn(activeUser);

    MessageResponse response = authService.verifyEmail(tokenStr);

    assertThat(response.getMessage()).contains("verified");
    verify(emailVerificationTokenRepository).save(argThat(t -> t.getVerified()));
  }

  @Test
  void verifyEmail_throwsForExpiredToken() {
    // makes sure that when an expired email verification token is provided to the AuthService's
    // verifyEmail method,
    // the method throws an IllegalArgumentException with a message indicating that the token has
    // expired,
    // ensuring that expired tokens cannot be used for email verification.
    String tokenStr = UUID.randomUUID().toString();
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .token(tokenStr)
            .userId(activeUser.getId())
            .expiresAt(LocalDateTime.now().minusHours(1))
            .verified(false)
            .build();

    when(emailVerificationTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> authService.verifyEmail(tokenStr))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("token expired");
  }

  @Test
  void verifyEmail_throwsForAlreadyUsedToken() {
    // this test checks that when an email verification token that has already been used (i.e. its
    // verified property is true)
    // is provided to the AuthService's verifyEmail method,
    String tokenStr = UUID.randomUUID().toString();
    EmailVerificationToken token =
        EmailVerificationToken.builder()
            .token(tokenStr)
            .userId(activeUser.getId())
            .expiresAt(
                LocalDateTime.now().plusHours(1)) // the token is not expired, 1 hour in the future
            .verified(true)
            .build();

    when(emailVerificationTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> authService.verifyEmail(tokenStr))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("token already used");
  }

  @Test
  void verifyEmail_throwsForUnknownToken() {
    // throws an IllegalArgumentException with a message indicating that the token was not found
    // when an unknown email verification token
    // is provided to the AuthService's verifyEmail method,
    // ensuring that invalid tokens cannot be used for email verification and that the system
    // properly
    // handles cases where the token does not exist in the repository.
    when(emailVerificationTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("token not found");
  }

  // login
  // these tests check the behavior of the AuthService's login method under various conditions,
  // such as valid credentials, wrong password, unknown email, account locked,
  // and the effects of login attempts on the user's account status.

  @Test
  void login_returnsTokenForValidCredentials() {
    // this test checks that when valid credentials are provided to the AuthService's login method,
    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword("Password123!");

    when(userRepository.findByEmail("bob@momentum.co.za")).thenReturn(Optional.of(activeUser));
    when(passwordEncoder.matches("Password123!", activeUser.getPasswordHash())).thenReturn(true);
    when(jwtService.generateToken(activeUser, 1)).thenReturn("mock-jwt-token");
    when(userRepository.save(any())).thenReturn(activeUser);

    AuthResponse response = authService.login(request);

    assertThat(response.getToken()).isEqualTo("mock-jwt-token");
    // this checks that the token returned by the login method matches the mocked token generated by
    // the jwtService,
    // confirming that the login process successfully generates and returns a JWT token for valid
    // credentials.
    assertThat(response.getUser().getEmail()).isEqualTo("bob@momentum.co.za");
    // this verifies that the user information included in the AuthResponse contains the correct
    // email, confirming that the response includes the expected user details for the authenticated
    // user.
  }

  @Test
  void login_throwsForWrongPassword() {
    // this test checks that when an incorrect password is provided for a valid email, the
    // AuthService's login method
    // throws an IllegalArgumentException with a message indicating invalid credentials,
    // ensuring that the login process properly validates the password and does not allow access
    // with incorrect credentials.
    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword(
        "WrongPassword!"); // the password does not match the expected password for the user, which
    // should trigger the invalid credentials error in the login method

    when(userRepository.findByEmail("bob@momentum.co.za")).thenReturn(Optional.of(activeUser));
    when(passwordEncoder.matches("WrongPassword!", activeUser.getPasswordHash())).thenReturn(false);
    when(userRepository.save(any())).thenReturn(activeUser);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid credentials");
  }

  @Test
  void login_throwsForUnknownEmail() {
    // this test checks that when an email that does not exist in the system is provided to the
    // AuthService's login method,
    // the method throws an IllegalArgumentException with a message indicating invalid credentials,
    // ensuring that the login process properly handles cases where the email is not found and does
    // not allow access with unknown emails.
    AuthRequest request = new AuthRequest();
    request.setEmail("ghost@momentum.co.za");
    request.setPassword("Password123!");

    when(userRepository.findByEmail("ghost@momentum.co.za")).thenReturn(Optional.empty());
    // the userRepository is mocked to return an empty Optional when searching for the unknown
    // email,
    // simulating the case where the email does not exist in the database

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid credentials");
  }

  @Test
  void login_throwsWhenAccountLocked() {
    activeUser.setLoginAttempts(
        5); // the user's login attempts are set to 5, which is the threshold for locking the
    // account according to the AuthService's login logic

    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword("Password123!");

    when(userRepository.findByEmail("bob@momentum.co.za")).thenReturn(Optional.of(activeUser));

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("account locked");
  }

  @Test
  void login_incrementsLoginAttemptsOnBadPassword() {
    // this test checks that when an incorrect password is provided to the AuthService's login
    // method,
    // the method increments the user's loginAttempts count by 1,
    // and throws an IllegalArgumentException with a message indicating invalid credentials,
    // ensuring that the login process properly tracks failed login attempts and updates the user's
    // account status
    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword("Wrong!");

    when(userRepository.findByEmail("bob@momentum.co.za")).thenReturn(Optional.of(activeUser));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    when(userRepository.save(captor.capture())).thenReturn(activeUser);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(captor.getValue().getLoginAttempts()).isEqualTo(1);
  }

  @Test
  void login_resetsLoginAttemptsOnSuccess() {
    // this checks that when valid credentials are provided to the AuthService's login method, the
    // method resets the user's loginAttempts count to 0,
    // and returns a valid AuthResponse with a JWT token, ensuring that successful logins properly
    // reset the failed login attempts and allow access to the user.
    activeUser.setLoginAttempts(3);

    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword("Password123!");

    when(userRepository.findByEmail("bob@momentum.co.za")).thenReturn(Optional.of(activeUser));
    when(passwordEncoder.matches("Password123!", activeUser.getPasswordHash())).thenReturn(true);
    when(jwtService.generateToken(activeUser, 1)).thenReturn("token");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    when(userRepository.save(captor.capture())).thenReturn(activeUser);

    authService.login(request);

    assertThat(captor.getValue().getLoginAttempts()).isEqualTo(0);
  }

  // logout
  // these tests check that the AuthService's logout method properly blacklists the provided JWT
  // token,
  // regardless of whether the token string includes the "Bearer " prefix or not,
  // ensuring that the logout functionality correctly handles different token formats and
  // effectively blacklists the token to prevent further use.
  @Test
  void logout_blacklistsTokenWithBearerPrefix() {
    // this test checks that when a JWT token string that includes the "Bearer " prefix is provided
    // to the AuthService's logout method,
    // the method correctly extracts the token from the string and calls the tokenBlacklistService's
    // blacklistToken method with the extracted token,
    authService.logout("Bearer some-jwt-token");
    verify(tokenBlacklistService).blacklistToken("some-jwt-token");
  }

  @Test
  void logout_blacklistsTokenWithoutBearerPrefix() {
    // this test checks that when a JWT token string without the "Bearer " prefix is provided to the
    // AuthService's logout method,
    // the method directly calls the tokenBlacklistService's blacklistToken method with the provided
    // token string,
    // ensuring that the logout functionality correctly handles token strings without the prefix and
    // still effectively blacklists the token
    authService.logout("some-jwt-token");
    verify(tokenBlacklistService).blacklistToken("some-jwt-token");
  }

  //  forgotPassword

  @Test
  void forgotPassword_alwaysReturnsSuccessForUnknownEmail() {
    // this test checks that when an email that does not exist in the system is provided to the
    // AuthService's forgotPassword method,
    // the method still returns a MessageResponse with a message indicating that a reset link has
    // been sent, without throwing an exception,
    // ensuring that the forgot password functionality does not reveal whether an email exists in
    // the system and always returns a success message to prevent information disclosure about user
    // accounts.
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("ghost@momentum.co.za");

    when(userRepository.findByEmail("ghost@momentum.co.za")).thenReturn(Optional.empty());

    MessageResponse response = authService.forgotPassword(request);

    assertThat(response.getMessage()).contains("reset link sent");
    verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
  }

  @Test
  void forgotPassword_sendsEmailWhenUserExists() {
    // this test checks that when a valid email that exists in the system is provided to the
    // AuthService's forgotPassword method,
    // the method generates a password reset token, saves it to the repository, and calls the
    // emailService's sendPasswordResetEmail
    // method with the correct parameters to send the password reset email to the user,
    // ensuring that the forgot password functionality properly handles valid requests and initiates
    // the password reset process for existing users.
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("bob@momentum.co.za");

    when(userRepository.findByEmail("bob@momentum.co.za")).thenReturn(Optional.of(activeUser));
    when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    authService.forgotPassword(request);

    verify(emailService).sendPasswordResetEmail(eq("bob@momentum.co.za"), eq("Bob"), anyString());
  }

  //   @Test
  //   void forgotPassword_returnsSuccessEvenWhenUserExists() {
  //     // this test checks that when a valid email that exists in the system is provided to the
  //     // AuthService's forgotPassword method,
  //     // the method still returns a MessageResponse with a message indicating that a reset link
  // has
  //     // been sent, without throwing an exception,
  //     // ensuring that the forgot password functionality always returns a success message
  // regardless
  //     // of whether the email exists,
  //     // to prevent information disclosure about user accounts while still initiating the
  // password
  //     ForgotPasswordRequest request = new ForgotPasswordRequest();
  //     request.setEmail("bob@momentum.co.za");

  //     when(userRepository.findByEmail("bob@momentum.co.za")).thenReturn(Optional.of(activeUser));
  //     when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

  //     MessageResponse response = authService.forgotPassword(request);

  //     assertThat(response.getMessage()).isNotBlank();
  //   }
}
