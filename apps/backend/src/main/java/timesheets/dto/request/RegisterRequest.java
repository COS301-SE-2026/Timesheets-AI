// package timesheets.dto.request;

// import jakarta.validation.constraints.Email;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Pattern;
// import lombok.Data;

// // a simple request object for registration, which contains first name, last name, email, and
// // password.
// // we will validate the email format and password strength using annotations.
// @Data
// public class RegisterRequest {

//   @NotBlank(message = "first name is required")
//   private String firstName;

//   @NotBlank(message = "last name is required")
//   private String lastName;

//   @NotBlank(message = "email is required")
//   @Email(message = "invalid email format") // must be .momentum.co.za email
//   private String email;

//   @NotBlank(message = "password is required")
//   @Pattern(
//       regexp =
//           "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$", // at least 8 characters, one
// uppercase,
//       // one number, and one special character
//       message =
//           "password must be at least 8 characters with one uppercase, one number, and one special
// character")
//   private String password;
// }
