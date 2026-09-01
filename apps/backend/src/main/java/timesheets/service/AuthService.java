package timesheets.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import exception.AuthException;
import exception.AuthException.ErrorCode;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import lombok.RequiredArgsConstructor;
import timesheets.domain.EmailVerificationToken;
import timesheets.domain.PasswordResetToken;
import timesheets.domain.User;
import timesheets.domain.UserIdentityProvider;
import timesheets.domain.UserMfa;
import timesheets.dto.request.AuthRequest;
import timesheets.dto.request.GoogleAuthRequest;
import timesheets.dto.request.RegisterRequest;
import timesheets.dto.response.AuthResponse;
import timesheets.dto.response.MessageResponse;
import timesheets.dto.response.RegisterResponse;
import timesheets.enums.UserStatus;
import timesheets.repository.EmailVerificationTokenRepository;
import timesheets.repository.UserIdentityProviderRepository;
import timesheets.repository.UserMfaRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;
import timesheets.util.TotpUtils;

// import timesheets.dto.request.ForgotPasswordRequest;
// import timesheets.dto.request.ResetPasswordRequest;

// this is the file that has all my business logic, the control will call the service and the
// service will call repo
// it has all the functions for authentication, registration, email verification, password reset,
// and logout
// note some of these may be integrated after Demo 1 depending on time,
// but I have them here for completeness and to show the full implementation of authentication
// features
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final UserMfaRepository userMfaRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserIdentityProviderRepository userIdentityProviderRepository;
  private final PasswordEncoder passwordEncoder;

  private final EmailService emailService;

  private final TotpUtils totpUtils;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;

  private final TokenBlacklistService tokenBlacklistService;
  private final JwtService jwtService;
  private final SecurityUtils securityUtils;
  private final TimerService timerService;

  // private final OtpService otpService;
  // private final PasswordResetTokenRepository passwordResetTokenRepository;

  @Value("${app.google.client-id}")
  private String googleClientId;

  private static final String[] ACCEPTED_DOMAINS = {
    "momentum.co.za", "momentum.com", "gmail.com", "cs.up.ac.za", "outlook.com"
  };
  private static final int MAX_LOGIN_ATTEMPTS = 5;
  private static final String ISSUER = "Timesheets AI";

  @Transactional
  public RegisterResponse register(RegisterRequest request) {

    // validate email domain
    if (!isAcceptedDomain(request.getEmail())) {
      throw new AuthException(ErrorCode.EMAIL_DOMAIN);
    }

    // this should check if the user exists
    Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

    // checks to see if the user exists
    if (existingUser.isPresent()) {
      User user = existingUser.get();

      // if the email already exists then they cannot register again
      if (Boolean.TRUE.equals(user.getEmailVerified())) {
        throw new AuthException(ErrorCode.EMAIL_EXISTS);
      }

      // if they are not verified, then the email verification is sent again
      emailVerificationTokenRepository.deleteByUserId(user.getId());

      // generate verification token
      String token = UUID.randomUUID().toString();
      EmailVerificationToken verificationToken =
          EmailVerificationToken.builder()
              .token(token)
              .userId(user.getId())
              .expiresAt(LocalDateTime.now().plusHours(24))
              .build();

      emailVerificationTokenRepository.save(verificationToken);

      // send verification email
      emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);

      // return RegisterResponse.builder()
      //     .id(user.getId().toString())
      //     .email(user.getEmail())
      //     .firstName(user.getFirstName())
      //     .lastName(user.getLastName())
      //     .createdAt(user.getCreatedAt())
      //     .message("Verification email sent. Please check your inbox.")
      //     .build();

      // DEMO 2
      return RegisterResponse.builder()
          .id(user.getId().toString())
          .email(user.getEmail())
          .firstName(user.getFirstName())
          .lastName(user.getLastName())
          .createdAt(user.getCreatedAt())
          .message("Account created successfully.")
          .build();
    }

    // create user
    User user =
        User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .emailVerified(false)
            .createdAt(LocalDateTime.now())
            .build();

    user = userRepository.save(user);

    // generate verification token
    String token = UUID.randomUUID().toString();
    EmailVerificationToken verificationToken =
        EmailVerificationToken.builder()
            .token(token)
            .userId(user.getId())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();

    emailVerificationTokenRepository.save(verificationToken);

    // send verification email
    emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);

    return RegisterResponse.builder()
        .id(user.getId().toString())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .createdAt(user.getCreatedAt())
        .message("Verification email sent. Please check your inbox.")
        .build();
  }

  @Transactional
  public MessageResponse verifyEmail(String token) {
    EmailVerificationToken verificationToken =
        emailVerificationTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new AuthException(ErrorCode.TOKEN_NOT_FOUND));

    if (verificationToken.getVerifiedAt() != null) {
      throw new AuthException(ErrorCode.TOKEN_USED);
    }

    if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new AuthException(ErrorCode.TOKEN_EXPIRED);
    }

    // mark token as verified
    verificationToken.setVerifiedAt(LocalDateTime.now());
    emailVerificationTokenRepository.save(verificationToken);

    // update user email verification status
    User user =
        userRepository
            .findById(verificationToken.getUserId())
            .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

    user.setEmailVerified(true);

    userRepository.save(user);
    // userRepository.saveAndFlush(user);

    return new MessageResponse("Email verified successfully", "/dashboard");
  }

  /*
   * I am using no rollback such that there is no rollback for login failures.
   * I want the login attempts to still register in the DB
   */
  @Transactional(noRollbackFor = {AuthException.class})
  public AuthResponse login(AuthRequest request) {
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

    // check if the user is an SSO user(this means that they have no password)
    if (user.getPasswordHash() == null) {
      boolean hasGoogleProvider =
          userIdentityProviderRepository
              .findByProviderAndUserId("GOOGLE", user.getId())
              .isPresent();

      if (hasGoogleProvider) {
        throw new AuthException(ErrorCode.SSO_USER);
      } else {
        throw new AuthException(ErrorCode.ACCOUNT_NOT_CONFIGURED);
      }
    }
    // check if the account is locked, and for how long
    if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
      long minutesRemain =
          java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes();
      throw new AuthException(ErrorCode.ACCOUNT_LOCKED, minutesRemain);
    }

    // verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      // increment failed login attempts
      int attempts =
          (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;

      // System.out.println("Incrementing attempts to: " + attempts);
      user.setFailedLoginAttempts(attempts);

      // System.out.println("Saved user");

      if (attempts >= MAX_LOGIN_ATTEMPTS) {
        user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        throw new AuthException(ErrorCode.ACCOUNT_LOCKED, 30);
      }

      userRepository.save(user);
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }

    // reset login attempts on successful password verification
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    userRepository.save(user);

    // TODO
    // if (!Boolean.TRUE.equals(user.getEmailVerified())) {
    //   throw new IllegalStateException("please verify your email before logging in");
    // }

    // check if MFA is enabled
    boolean mfaEnabled =
        userMfaRepository.findByUserId(user.getId()).map(UserMfa::getIsEnabled).orElse(false);

    return generateAuthResponse(user, mfaEnabled);
  }

  @Transactional
  public MessageResponse changePassword(UUID userId, String currentPassword, String newPassword, String confirmPassword) {
    
    //validating that a user 
    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    //if a user uses SSO then they cannot change their pasword
    if (user.getPasswordHash() == null) {
      throw new StateConflictException("This account uses SSO. Cannot change password.");
    }

    //this should verify the current password
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new BadRequestException("Current password is incorrect");
    }

    //passwords should not match
    if (!newPassword.equals(confirmPassword)) {
      throw new BadRequestException("Passwords do not match");
    }

    //checking the password strength
    if (newPassword.length() < 8) {
      throw new BadRequestException("New password must be at least 8 characters");
    }

    //this should update the password
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    //this will be what logs the user activity
    log.info("Password changed for user: {}", user.getEmail());

    return new MessageResponse("Password changed successfully");
  }

  @Transactional
  public MessageResponse forgotPassword(String email) {
    // Always return success to prevent email enumeration
    userRepository.findByEmail(email).ifPresent(user -> {

      String token = UUID.randomUUID().toString();
      // Save token to password_reset_tokens table
      // passwordResetTokenRepository.save(resetToken);

      emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);

      log.info("Password reset requested for user: {}", user.getEmail());
    });

    return new MessageResponse("Password reset link sent to your email if the account exists");
  }

  @Transactional
  public MessageResponse resetPassword(ResetPasswordRequest request) {

    PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken()).orElseThrow(() -> new ResourceNotFoundException("Password reset token not found"));

    if (resetToken.getUsed()) {
    throw new StateConflictException("Token already used");
    }

    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new AuthException(ErrorCode.TOKEN_EXPIRED);
    }

    // mark token as used
    resetToken.setUsed(true);
    passwordResetTokenRepository.save(resetToken);

    // update user password
    User user = userRepository.findById(resetToken.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    return new MessageResponse("Password reset successfully", "/login");
  }

  @Transactional
  public void logout(String token) {

    // extract token from bearer string if needed
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }

    tokenBlacklistService.blacklistToken(token);

    try {
      UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
      timerService.pauseTimerForLogout(workspaceMemberId);
    } catch (Exception e) {
      log.debug("No active timer to pause during logout");    
     }
  }

  @Transactional
  public AuthResponse googleAuth(GoogleAuthRequest request) {
    // first need to see if Google ID token is valif

    GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());

    String googleId = payload.getSubject();
    String email = payload.getEmail();
    String firstName = (String) payload.get("given_name");
    String lastName = (String) payload.get("family_name");
    String avatarUrl = (String) payload.get("picture");
    Boolean emailVerified = payload.getEmailVerified();

    // see if the identity provider exists
    return userIdentityProviderRepository
        .findByProviderAndProviderUserId("GOOGLE", googleId)
        .map(
            identity -> {
              User user =
                  userRepository
                      .findById(identity.getUserId())
                      .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
              return generateAuthResponse(user, false);
            })
        .orElseGet(
            () -> {
              return userRepository
                  .findByEmail(email)
                  .map(
                      user -> {
                        linkGoogleIdentity(user, googleId);
                        return generateAuthResponse(user, false);
                      })
                  .orElseGet(
                      () -> {
                        User newUser = createUserFromGoogle(payload);
                        linkGoogleIdentity(newUser, googleId);
                        return generateAuthResponse(newUser, false);
                      });
            });
  }

  // ! helper functions
  // helper func to see if the email is in the accepted domain
  private boolean isAcceptedDomain(String email) {
    String domain = email.substring(email.indexOf("@") + 1);
    for (String acceptedDomain : ACCEPTED_DOMAINS) {
      if (domain.equalsIgnoreCase(acceptedDomain)) {
        return true;
      }
    }
    return false;
  }

  // helper function that helps generate the authorisation response
  private AuthResponse generateAuthResponse(User user, boolean requiresMfa) {
    // need to see if Mfa is enabled
    boolean mfaEnabled =
        userMfaRepository.findByUserId(user.getId()).map(UserMfa::getIsEnabled).orElse(false);

    // the workspace roles are taken from memberships
    List<String> roles =
        workspaceMemberRepository.findByUserId(user.getId()).stream()
            .map(membership -> "ROLE_" + membership.getRole().name())
            .collect(Collectors.toList());
    if (roles.isEmpty()) {
      roles = List.of("ROLE_USER");
    }

    // we should only generate the token only if MFA is not required
    String token = null;
    LocalDateTime expiresAt = null;

    if (!requiresMfa) {
      int expirationDays = 1;
      token = jwtService.generateToken(user, expirationDays);
      expiresAt = LocalDateTime.now().plusDays(expirationDays);
    }

    AuthResponse.UserInfo userInfo =
        AuthResponse.UserInfo.builder()
            .id(user.getId().toString())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .emailVerified(user.getEmailVerified())
            .avatarUrl(user.getAvatarUrl())
            .roles(roles)
            .mfaEnabled(mfaEnabled)
            .build();

    return AuthResponse.builder()
        .token(token)
        .expiresAt(expiresAt)
        .user(userInfo)
        .requiresMfa(requiresMfa && mfaEnabled)
        .build();
  }

  // helper func to see if the google token is valid
  private GoogleIdToken.Payload verifyGoogleToken(String idToken) {

    // FOR DEVELOPMENT
    if ("swagger-test".equals(idToken)) {
      GoogleIdToken.Payload payload = new GoogleIdToken.Payload();

      payload.setSubject("google-test-user-123");
      payload.setEmail("thabang.siduke@momentum.co.za");
      payload.put("given_name", "Thabang");
      payload.put("family_name", "Siduke");
      payload.put("picture", "https://www.magnific.com/free-photos-vectors/avatar-logo");

      return payload;
    }

    // this will be what the actual google verification will be ACTUAL PRODUCTION
    try {
      GoogleIdTokenVerifier verifier =
          new GoogleIdTokenVerifier.Builder(
                  new NetHttpTransport(), GsonFactory.getDefaultInstance())
              .setAudience(Collections.singletonList(googleClientId))
              .build();

      GoogleIdToken idTokenObj = verifier.verify(idToken);
      if (idTokenObj == null) {
        throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
      }
      return idTokenObj.getPayload();
    } catch (Exception e) {
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }
  }

  // helper func that creates a user from a google token
  private User createUserFromGoogle(GoogleIdToken.Payload payload) {
    String firstName =
        payload.get("given_name") != null ? (String) payload.get("given_name") : "Google User";
    String lastName =
        payload.get("family_name") != null ? (String) payload.get("family_name") : "Unknown";

    User user =
        User.builder()
            .email(payload.getEmail())
            .firstName(firstName)
            .lastName(lastName)
            .avatarUrl((String) payload.get("picture"))
            .emailVerified(Boolean.TRUE.equals(payload.getEmailVerified()))
            .passwordHash(null)
            .status(UserStatus.ACTIVE)
            .build();

    return userRepository.save(user);
  }

  private void linkGoogleIdentity(User user, String googleId) {
    UserIdentityProvider identity =
        UserIdentityProvider.builder()
            .userId(user.getId())
            .provider("GOOGLE")
            .providerUserId(googleId)
            .email(user.getEmail())
            .build();

    userIdentityProviderRepository.save(identity);
  }
}
