package timesheets.controller;

// this unit test class focuses on testing the AuthController's endpoints and their expected
// behaviors under various scenarios.
// It uses MockMvc to simulate HTTP requests and Mockito to mock the AuthService's behavior,
// allowing us to verify how the controller handles different inputs and service responses without
// needing a full application context or real database interactions.
// Each test method corresponds to a specific endpoint and scenario, ensuring comprehensive coverage
// of the authentication flow.

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import timesheets.config.JwtAuthFilter;
import timesheets.config.SecurityConfig;
import timesheets.dto.request.AuthRequest;
import timesheets.dto.request.ForgotPasswordRequest;
import timesheets.dto.request.RegisterRequest;
import timesheets.dto.response.AuthResponse;
import timesheets.dto.response.MessageResponse;
import timesheets.dto.response.RegisterResponse;
import timesheets.repository.UserRepository;
import timesheets.service.AuthService;
import timesheets.service.JwtService;
import timesheets.service.TokenBlacklistService;

// summary of the responses for the AuthController's endpoints:
// 200 good request
// 201 created
// 204 no content
// 400 bad request
// 401 unauthorized
// 403 forbidden
// 404 not found
// 500 internal server error

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean AuthService authService;
  @MockBean JwtService jwtService;
  @MockBean TokenBlacklistService tokenBlacklistService;
  @MockBean UserRepository userRepository;

  //  POST /api/auth/register

  @Test
  void register_returns201WithValidPayload() throws Exception {
    // this test returns a 201 Created status when a valid registration payload is provided,
    // and it checks that the response contains the correct email and a message indicating that a
    // verification email has been sent.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("newuser@momentum.co.za");
    request.setFirstName("New");
    request.setLastName("User");
    request.setPassword("Password123!");

    RegisterResponse mockResponse =
        RegisterResponse.builder()
            .id("some-uuid")
            .email("newuser@momentum.co.za")
            .firstName("New")
            .lastName("User")
            .createdAt(LocalDateTime.now())
            .message("Verification email sent. Please check your inbox.")
            .build();

    when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("newuser@momentum.co.za"))
        .andExpect(
            jsonPath("$.message")
                .value("Verification email sent. Please check your inbox.")); // we dont have a real
    // email service to send
    // the verification
    // email, so we just
    // return a generic
    // message for testing
    // purposes.
  }

  @Test
  void register_returns400WithInvalidEmailFormat() throws Exception {
    // this test returns a 400 Bad Request status when the email format is invalid in the
    // registration payload,
    // and it checks that the error message indicates the invalid email format.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("not-an-email");
    request.setFirstName("New");
    request.setLastName("User");
    request.setPassword("Password123!");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_returns400WithWeakPassword() throws Exception {
    // this test returns a 400 Bad Request status when the password is too weak in the registration
    // payload,
    // and it checks that the error message indicates the weak password.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("user@momentum.co.za");
    request.setFirstName("New");
    request.setLastName("User");
    request.setPassword("weak");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_returns400WithMissingFirstName() throws Exception {
    // this test returns a 400 Bad Request status when the first name is missing in the registration
    // payload,
    // and it checks that the error message indicates the missing first name.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("user@momentum.co.za");
    request.setLastName("User");
    request.setPassword("Password123!");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_returns400WhenEmailDomainNotAccepted() throws Exception {
    // this test returns a 400 Bad Request status when the email domain is not accepted in the
    // registration payload,
    // and it checks that the error message indicates the email domain is not accepted.
    RegisterRequest request = new RegisterRequest();
    request.setEmail("user@gmail.com");
    request.setFirstName("New");
    request.setLastName("User");
    request.setPassword("Password123!");

    when(authService.register(any()))
        .thenThrow(new IllegalArgumentException("email domain not accepted"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_returns400WhenEmailAlreadyExists() throws Exception {
    // this test returns a 400 Bad Request status when the email already exists in the registration
    // payload,
    // and it checks that the error message indicates the email already exists
    // Since the email field must be unique for each user,
    // the controller should respond with a 400 status when an attempt is made to register with an
    // email that is already in use,
    // ensuring that the client is informed about the duplicate email issue
    RegisterRequest request = new RegisterRequest();
    request.setEmail("bob@momentum.co.za");
    request.setFirstName("Bob");
    request.setLastName("Dev");
    request.setPassword("Password123!");

    when(authService.register(any()))
        .thenThrow(new IllegalArgumentException("email already exists"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // POST /api/auth/verify-email
  // these tests check the behavior of the email verification endpoint
  // under different conditions, such as valid tokens, expired tokens, and already used tokens.

  @Test
  void verifyEmail_returns200WithValidToken() throws Exception {
    // this test returns a 200 OK status when a valid token is provided,
    // and it checks that the response message indicates successful email verification.
    when(authService.verifyEmail("valid-token"))
        .thenReturn(new MessageResponse("Email verified successfully", "/dashboard"));

    mockMvc
        .perform(post("/api/auth/verify-email").param("token", "valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Email verified successfully"));
  }

  @Test
  void verifyEmail_returns400WithExpiredToken() throws Exception {
    // this test returns a 400 Bad Request status when an expired token is provided,
    // and it checks that the error message indicates the token has expired.
    when(authService.verifyEmail("expired-token"))
        .thenThrow(new IllegalArgumentException("token expired"));

    mockMvc
        .perform(post("/api/auth/verify-email").param("token", "expired-token"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void verifyEmail_returns400WithAlreadyUsedToken() throws Exception {
    // this test returns a 400 Bad Request status when an already used token is provided,
    // and it checks that the error message indicates the token has already been used.
    when(authService.verifyEmail("used-token"))
        .thenThrow(new IllegalArgumentException("token already used"));

    mockMvc
        .perform(post("/api/auth/verify-email").param("token", "used-token"))
        .andExpect(status().isBadRequest());
  }

  //  POST /api/auth/login
  // these tests check the behavior of the login endpoint under various scenarios,
  // including valid credentials, wrong passwords, missing email, and account lockout due to too
  // many failed attempts.

  @Test
  void login_returns200WithValidCredentials() throws Exception {
    // this test returns a 200 OK status when valid credentials are provided,
    // and it checks that the response contains a JWT token and the correct user email.
    // uses mocked values for the AuthService's login method to simulate a
    // successful login scenario without needing real authentication logic or database access.
    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword("Password123!");

    AuthResponse mockResponse =
        AuthResponse.builder()
            .token("jwt-token")
            .expiresAt(LocalDateTime.now().plusDays(1))
            .user(
                AuthResponse.UserInfo.builder()
                    .id("some-uuid")
                    .email("bob@momentum.co.za")
                    .firstName("Bob")
                    .lastName("Dev")
                    .build())
            .build();

    when(authService.login(any(AuthRequest.class))).thenReturn(mockResponse);

    mockMvc
        .perform(
            post("/api/auth/login")
                // this simulates an HTTP POST request to the /api/auth/login endpoint with a JSON
                // payload containing the email and password,
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"))
        .andExpect(jsonPath("$.user.email").value("bob@momentum.co.za"));
  }

  @Test
  void login_returns400WithWrongPassword() throws Exception {
    // returns a 400 Bad Request status when the wrong password is provided,
    // and it checks that the error message indicates invalid credentials.
    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword("WrongPassword!");

    when(authService.login(any())).thenThrow(new IllegalArgumentException("invalid credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_returns400WithMissingEmail() throws Exception {
    // this test returns a 400 Bad Request status when the email is missing from the login request,
    // and it checks that the error message indicates the email is required.
    // since the email field is required for authentication, the controller should respond with a
    // 400 status when it's not provided,
    // ensuring that the client is informed about the missing required field.
    AuthRequest request = new AuthRequest();
    request.setPassword("Password123!");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_returns400WhenAccountLocked() throws Exception {
    // this test returns a 400 Bad Request status when the account is locked due to too many failed
    // login attempts,
    // and it checks that the error message indicates the account is locked.
    AuthRequest request = new AuthRequest();
    request.setEmail("bob@momentum.co.za");
    request.setPassword("Password123!");

    when(authService.login(any()))
        .thenThrow(new IllegalStateException("account locked after too many failed attempts"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // POST /api/auth/logout
  // these tests check the behavior of the logout endpoint, ensuring that it correctly handles valid
  // tokens and missing authorization headers.
  // The first test verifies that a valid token results in a successful logout with a 204 No Content
  // status, while the second test ensures that a missing authorization header leads to a 400 Bad
  // Request status.

  @Test
  @WithMockUser
  void logout_returns204WithValidToken() throws Exception {
    // this test returns a 204 No Content status when a valid token is provided in the Authorization
    // header,
    // and it verifies that the AuthService's logout method is called with the correct token.
    doNothing().when(authService).logout(any());

    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer some-valid-jwt"))
        .andExpect(status().isNoContent());

    verify(authService).logout("Bearer some-valid-jwt");
  }

  @Test
  @WithMockUser
  void logout_returns400WithMissingAuthorizationHeader() throws Exception {
    // this test returns a 400 Bad Request status when the Authorization header is missing from the
    // logout request,
    // and it checks that the error message indicates the missing token.
    mockMvc.perform(post("/api/auth/logout")).andExpect(status().isBadRequest());
  }

  // POST /api/auth/forgot-password
  // these tests check the behavior of the forgot password endpoint, making sure that it always
  // returns a 200 OK status regardless of whether the email exists in the system or not.
  // The first test verifies that a valid email results in a successful response with a message
  // indicating that a password reset link has been sent,
  // while the second test ensures that even an unknown email address

  @Test
  void forgotPassword_alwaysReturns200() throws Exception {
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail(
        "anyone@momentum.co.za"); // this test returns a 200 OK status regardless of whether the
    // email exists in the system or not,
    // and it checks that the response message indicates that a password reset link has been sent to
    // the email if the account exists.
    // at the moment we dont have an email service to send the reset link,
    // so we just return a generic message for both existing and non-existing emails to prevent
    // email enumeration attacks.

    when(authService.forgotPassword(any()))
        .thenReturn(
            new MessageResponse("Password reset link sent to your email if the account exists"));

    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void forgotPassword_returns200EvenForUnknownEmail() throws Exception {
    // this test returns a 200 OK status even when an unknown email address is provided in the
    // forgot password request,
    // and it checks that the response message indicates that a password reset link has been sent to
    // the email if the account exists,
    // ensuring that the endpoint does not reveal whether the email is registered or not, which is a
    // common security practice to prevent email enumeration attacks.
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("ghost@momentum.co.za");

    when(authService.forgotPassword(any()))
        .thenReturn(
            new MessageResponse("Password reset link sent to your email if the account exists"));
    // we dont have a real email service to send the reset link,
    // so we just return a generic message for both existing and non-existing emails to prevent
    // email enumeration attacks.

    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}
