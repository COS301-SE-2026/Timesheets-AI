package timesheets.controller;

import exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import timesheets.domain.User;
import timesheets.dto.request.AuthRequest;
import timesheets.dto.request.GoogleAuthRequest;
import timesheets.dto.request.MfaDisableRequest;
import timesheets.dto.request.MfaLoginVerifyRequest;
import timesheets.dto.request.MfaVerifyRequest;
import timesheets.dto.request.MicrosoftAuthRequest;
import timesheets.dto.request.PasswordRequest;
import timesheets.dto.request.RegisterRequest;
import timesheets.dto.response.AuthResponse;
import timesheets.dto.response.MessageResponse;
import timesheets.dto.response.MfaSetupResponse;
import timesheets.dto.response.RegisterResponse;
import timesheets.security.CustomUserDetails;
import timesheets.service.AuthService;
import timesheets.service.JwtService;
import timesheets.service.MfaService;

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
  private final JwtService jwtService;
  private final MfaService mfaService;

  @Operation(
      summary = "Set up MFA",
      description = "Generates MFA setup information for the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "MFA setup generated successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MfaSetupResponse.class))),
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
  })
  @GetMapping("/mfa/setup")
  public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

    MfaSetupResponse response = mfaService.setup(userDetails.getUserId());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Verify MFA",
      description = "Verifies the MFA code and enables MFA for the user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "MFA enabled successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid MFA code")
  })
  @PostMapping("/mfa/verify")
  public ResponseEntity<MessageResponse> verifyMfa(
      Authentication authentication, @Valid @RequestBody MfaVerifyRequest request) {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

    mfaService.verifySetup(userDetails.getUserId(), request.getTotpCode());

    return ResponseEntity.ok(new MessageResponse("MFA enabled successfully"));
  }

  @Operation(summary = "Disable MFA", description = "Disables MFA for the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "MFA disabled successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid password")
  })
  @PostMapping("/mfa/disable")
  public ResponseEntity<MessageResponse> disableMfa(
      Authentication authentication, @Valid @RequestBody MfaDisableRequest request) {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

    mfaService.disable(userDetails.getUserId(), request.getPassword());

    return ResponseEntity.ok(new MessageResponse("MFA disabled successfully"));
  }

  @Operation(
      summary = "Verify MFA login",
      description = "Verifies the MFA code and completes the login.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "MFA login verified successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid or expired MFA challenge")
  })
  @PostMapping("/mfa/login/verify")
  public ResponseEntity<AuthResponse> verifyMfaLogin(
      @Valid @RequestBody MfaLoginVerifyRequest request) {
    String challengeToken = request.getChallengeToken();

    if (!jwtService.isMfaChallengeToken(challengeToken)
        || jwtService.isTokenExpired(challengeToken)) {
      throw new BadRequestException("MFA challenge is invalid or expired");
    }

    UUID userId = jwtService.extractUserId(challengeToken);

    User user = mfaService.verifyLogin(userId, request.getTotpCode());

    AuthResponse response = authService.completeMfaLogin(user);

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Register user",
      description = "Registers a new user account and sends an email verification link.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "User registered successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RegisterResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid registration request")
  })
  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    RegisterResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Verify email",
      description = "Verifies the user's email using a verification token.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Email verified successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
  })
  @PostMapping("/verify-email")
  public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
    MessageResponse response = authService.verifyEmail(token);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Log in",
      description = "Authenticates a user using their email and password.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Login successful",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class))),
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Forgot password",
      description = "Sends a password reset link if the account exists.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Password reset request processed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)))
  })
  @PostMapping("/forgot-password")
  public ResponseEntity<MessageResponse> forgotPassword(
      @Valid @RequestBody PasswordRequest.Forgot request) {
    MessageResponse response = authService.forgotPassword(request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Reset password",
      description = "Resets the user's password using a reset token.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Password reset successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid password reset request")
  })
  @PostMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(
      @Valid @RequestBody PasswordRequest.Reset request) {
    MessageResponse response = authService.resetPassword(request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Change password",
      description = "Changes the password of the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Password changed successfully",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid password")
  })
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

  @Operation(summary = "Log out", description = "Logs out the authenticated user.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Logout successful"),
    @ApiResponse(responseCode = "401", description = "User is not authenticated")
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
    authService.logout(authorization);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Google authentication",
      description = "Authenticates a user using their Google account.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Google authentication successful",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class))),
    @ApiResponse(responseCode = "401", description = "Google authentication failed")
  })
  @PostMapping("/google")
  public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
    AuthResponse response = authService.googleAuth(request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Microsoft authentication",
      description = "Authenticates a user using their Microsoft account.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Microsoft authentication successful",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class))),
    @ApiResponse(responseCode = "401", description = "Microsoft authentication failed")
  })
  @PostMapping("/microsoft")
  public ResponseEntity<AuthResponse> microsoftAuth(
      @Valid @RequestBody MicrosoftAuthRequest request) {
    AuthResponse response = authService.microsoftAuth(request);
    return ResponseEntity.ok(response);
  }

  // TODO: I am going to do MFA support here- pausing it for now so I can fix the
  // previous code changes
}
