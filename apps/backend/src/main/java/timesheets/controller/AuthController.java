package timesheets.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.request.AuthRequest;
import timesheets.dto.request.GoogleAuthRequest;
import timesheets.dto.request.PasswordRequest;
import timesheets.dto.request.RegisterRequest;
import timesheets.dto.response.AuthResponse;
import timesheets.dto.response.MessageResponse;
import timesheets.dto.response.RegisterResponse;
import timesheets.security.CustomUserDetails;
import timesheets.service.AuthService;

// import timesheets.dto.request.GoogleAuthRequest;
// import timesheets.dto.request.MfaVerifyRequest;

// import java.util.UUID;
// import timesheets.dto.request.ForgotPasswordRequest;
// import timesheets.dto.request.ResetPasswordRequest;

// the controller is the entry point for all HTTP requests from the frontend
// it receives requests, gives work to the service layer, and returns responses
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    RegisterResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // a user will verify their email after registering
  @PostMapping("/verify-email")
  public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
    MessageResponse response = authService.verifyEmail(token);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<MessageResponse> forgotPassword(
      @Valid @RequestBody PasswordRequest.Forgot request) {
    MessageResponse response = authService.forgotPassword(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(
      @Valid @RequestBody PasswordRequest.Reset request) {
    MessageResponse response = authService.resetPassword(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/change-password")
  public ResponseEntity<MessageResponse> changePassword(
      Authentication authentication, @Valid @RequestBody PasswordRequest.Change request) {

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

    MessageResponse response =
        authService.changePassword(
            userDetails.getUserId(),
            request.getCurrentPassword(),
            request.getNewPassword(),
            request.getConfirmPassword());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
    authService.logout(authorization);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/google")
  public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
    AuthResponse response = authService.googleAuth(request);
    return ResponseEntity.ok(response);
  }

  // TODO: I am going to do MFA support here- pausing it for now so I can fix the
  // previous code changes
}
