// package timesheets.service;

// import java.time.LocalDateTime;
// import java.util.UUID;
// import lombok.RequiredArgsConstructor;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import timesheets.domain.*;
// import timesheets.dto.request.AuthRequest;
// import timesheets.dto.request.ForgotPasswordRequest;
// import timesheets.dto.request.RegisterRequest;
// import timesheets.dto.request.ResetPasswordRequest;
// import timesheets.dto.response.AuthResponse;
// import timesheets.dto.response.MessageResponse;
// import timesheets.dto.response.RegisterResponse;
// import timesheets.repository.*;

// // this is the file that has all my business logic, the control will call the service and the
// // service will call repo
// // it has all the functions for authentication, registration, email verification, password reset,
// // and logout
// // note some of these may be integrated after Demo 1 depending on time,
// // but I have them here for completeness and to show the full implementation of authentication
// // features
// @Service
// @RequiredArgsConstructor
// public class AuthService {

//   private final UserRepository userRepository;
//   private final PasswordEncoder passwordEncoder;
//   private final JwtService jwtService;
//   private final EmailService emailService;
//   // private final OtpService otpService;
//   private final TokenBlacklistService tokenBlacklistService;
//   private final PasswordResetTokenRepository passwordResetTokenRepository;
//   private final EmailVerificationTokenRepository emailVerificationTokenRepository;

//   private static final String[] ACCEPTED_DOMAINS = {"momentum.co.za", "momentum.com"};
//   private static final int MAX_LOGIN_ATTEMPTS = 5;

//   @Transactional
//   public RegisterResponse register(RegisterRequest request) {
//     // validate email domain
//     if (!isAcceptedDomain(request.getEmail())) {
//       throw new IllegalArgumentException("email domain not accepted");
//     }

//     // check if email already exists
//     if (userRepository.existsByEmail(request.getEmail())) {
//       throw new IllegalArgumentException("email already exists");
//     }

//     // create user
//     User user =
//         User.builder()
//             // .id(UUID.randomUUID())
//             .email(request.getEmail())
//             .firstName(request.getFirstName())
//             .lastName(request.getLastName())
//             .passwordHash(passwordEncoder.encode(request.getPassword()))
//             .emailVerified(false)
//             .createdAt(LocalDateTime.now())
//             .build();

//     user = userRepository.save(user);

//     // generate verification token
//     String token = UUID.randomUUID().toString();
//     EmailVerificationToken verificationToken =
//         EmailVerificationToken.builder()
//             .token(token)
//             .userId(user.getId())
//             .expiresAt(LocalDateTime.now().plusHours(24))
//             .verified(false)
//             .build();

//     emailVerificationTokenRepository.save(verificationToken);

//     // send verification email
//     emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);

//     return RegisterResponse.builder()
//         .id(user.getId().toString())
//         .email(user.getEmail())
//         .firstName(user.getFirstName())
//         .lastName(user.getLastName())
//         .createdAt(user.getCreatedAt())
//         .message("Verification email sent. Please check your inbox.")
//         .build();
//   }

//   @Transactional
//   public MessageResponse verifyEmail(String token) {
//     EmailVerificationToken verificationToken =
//         emailVerificationTokenRepository
//             .findByToken(token)
//             .orElseThrow(() -> new IllegalArgumentException("token not found"));

//     if (verificationToken.getVerified()) {
//       throw new IllegalArgumentException("token already used");
//     }

//     if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
//       throw new IllegalArgumentException("token expired");
//     }

//     // mark token as verified
//     verificationToken.setVerified(true);
//     emailVerificationTokenRepository.save(verificationToken);

//     // update user email verification status
//     User user =
//         userRepository
//             .findById(verificationToken.getUserId())
//             .orElseThrow(() -> new IllegalArgumentException("user not found"));
//     user.setEmailVerified(true);
//     userRepository.save(user);

//     return new MessageResponse("Email verified successfully", "/dashboard");
//   }

//   @Transactional
//   public AuthResponse login(AuthRequest request) {
//     User user =
//         userRepository
//             .findByEmail(request.getEmail())
//             .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));

//     // check if account is locked
//     if (user.getLoginAttempts() != null && user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
//       throw new IllegalStateException("account locked after too many failed attempts");
//     }

//     // verify password
//     if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
//       // increment failed login attempts
//       user.setLoginAttempts((user.getLoginAttempts() == null ? 0 : user.getLoginAttempts()) + 1);
//       userRepository.save(user);
//       throw new IllegalArgumentException("invalid credentials");
//     }

//     // reset login attempts on successful password verification
//     user.setLoginAttempts(0);
//     userRepository.save(user);

//     // generate jwt token
//     int expirationDays = 1; // token expires in 1 day, can be configured as needed
//     String token = jwtService.generateToken(user, expirationDays);
//     LocalDateTime expiresAt = LocalDateTime.now().plusDays(expirationDays);

//     return AuthResponse.builder()
//         .token(token)
//         .expiresAt(expiresAt)
//         .user(
//             AuthResponse.UserInfo.builder()
//                 .id(user.getId().toString())
//                 .email(user.getEmail())
//                 .firstName(user.getFirstName())
//                 .lastName(user.getLastName())
//                 .build())
//         .build();
//   }

//   @Transactional
//   public MessageResponse forgotPassword(ForgotPasswordRequest request) {
//     // always return success to prevent email enumeration
//     userRepository
//         .findByEmail(request.getEmail())
//         .ifPresent(
//             user -> {
//               String token = UUID.randomUUID().toString();
//               PasswordResetToken resetToken =
//                   PasswordResetToken.builder()
//                       .token(token)
//                       .userId(user.getId())
//                       .expiresAt(LocalDateTime.now().plusHours(1))
//                       .used(false)
//                       .build();

//               passwordResetTokenRepository.save(resetToken);
//               emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
//             });

//     return new MessageResponse("Password reset link sent to your email if the account exists");
//   }

//   @Transactional
//   public MessageResponse resetPassword(ResetPasswordRequest request) {
//     PasswordResetToken resetToken =
//         passwordResetTokenRepository
//             .findByToken(request.getToken())
//             .orElseThrow(() -> new IllegalArgumentException("token not found"));

//     if (resetToken.getUsed()) {
//       throw new IllegalArgumentException("token already used");
//     }

//     if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
//       throw new IllegalArgumentException("token expired");
//     }

//     // mark token as used
//     resetToken.setUsed(true);
//     passwordResetTokenRepository.save(resetToken);

//     // update user password
//     User user =
//         userRepository
//             .findById(resetToken.getUserId())
//             .orElseThrow(() -> new IllegalArgumentException("user not found"));
//     user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
//     userRepository.save(user);

//     return new MessageResponse("Password reset successfully", "/login");
//   }

//   public void logout(String token) {
//     // extract token from bearer string if needed
//     if (token.startsWith("Bearer ")) {
//       token = token.substring(7);
//     }

//     tokenBlacklistService.blacklistToken(token);
//   }

//   private boolean isAcceptedDomain(String email) {
//     String domain = email.substring(email.indexOf("@") + 1);
//     for (String acceptedDomain : ACCEPTED_DOMAINS) {
//       if (domain.equalsIgnoreCase(acceptedDomain)) {
//         return true;
//       }
//     }
//     return false;
//   }
// }
