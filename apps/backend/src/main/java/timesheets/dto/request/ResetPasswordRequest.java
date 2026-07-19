// package timesheets.dto.request;

// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Pattern;
// import lombok.Data;

// // this is the request object for the reset password endpoint,
// // it needs the token that was sent to the user's email and the new password they want to
// // might not be used for demo 1 i'm not sure if frontend is adding it
// // added validation
// @Data
// public class ResetPasswordRequest {

//   @NotBlank(message = "token is required")
//   private String token;

//   @NotBlank(message = "new password is required")
//   @Pattern(
//       regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$",
//       message =
//           "password must be at least 8 characters with one uppercase, one number, and one special
// character")
//   private String newPassword;
// }
