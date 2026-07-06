// package timesheets.controller;

// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import timesheets.dto.request.AuthRequest;
// import timesheets.dto.request.ForgotPasswordRequest;
// import timesheets.dto.request.RegisterRequest;
// import timesheets.dto.request.ResetPasswordRequest;
// import timesheets.dto.response.AuthResponse;
// import timesheets.dto.response.MessageResponse;
// import timesheets.dto.response.RegisterResponse;
// import timesheets.service.AuthService;

// // the controller is the entry point for all HTTP requests from the frontend
// // it receives requests, gives work to the service layer, and returns responses
// @RestController
// @RequestMapping("/api/auth")
// @RequiredArgsConstructor
// public class AuthController {

//   private final AuthService authService;

//   @PostMapping("/register")
//   public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
//     RegisterResponse response = authService.register(request);
//     return ResponseEntity.status(HttpStatus.CREATED).body(response);
//   }

//   @PostMapping("/verify-email")
//   public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
//     MessageResponse response = authService.verifyEmail(token);
//     return ResponseEntity.ok(response);
//   }

//   @PostMapping("/login")
//   public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
//     AuthResponse response = authService.login(request);
//     return ResponseEntity.ok(response);
//   }

//   // optional for demo 1
//   @PostMapping("/forgot-password")
//   public ResponseEntity<MessageResponse> forgotPassword(
//       @Valid @RequestBody ForgotPasswordRequest request) {
//     MessageResponse response = authService.forgotPassword(request);
//     return ResponseEntity.ok(response);
//   }

//   // optional for demo 1
//   @PostMapping("/reset-password")
//   public ResponseEntity<MessageResponse> resetPassword(
//       @Valid @RequestBody ResetPasswordRequest request) {
//     MessageResponse response = authService.resetPassword(request);
//     return ResponseEntity.ok(response);
//   }

//   @PostMapping("/logout")
//   public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
//     authService.logout(authorization);
//     return ResponseEntity.noContent().build();
//   }
// }
