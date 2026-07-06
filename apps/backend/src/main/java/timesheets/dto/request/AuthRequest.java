// package timesheets.dto.request;

// import jakarta.validation.constraints.Email;
// import jakarta.validation.constraints.NotBlank;
// import lombok.Data;

// // a simple request object for login, which only contains email and password, and an optional
// // rememberMe field for 30-day session.

// @Data
// public class AuthRequest {

//   @NotBlank(message = "email is required")
//   @Email(message = "invalid email format")
//   private String email;

//   @NotBlank(message = "password is required")
//   private String password;

//   // commented this out for now, we dont have this in the db.
//   // private Boolean rememberMe; // for 30-day session
// }
