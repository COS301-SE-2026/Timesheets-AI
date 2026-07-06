package timesheets.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// a simple request object for registration, which contains first name, last name, email, and
// password.
// we will validate the email format and password strength using annotations.
@Data
public class RegisterRequest {

  @NotBlank(message = "First name is required")
  private String firstName;

  @NotBlank(message = "Last name is required")
  private String lastName;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format") // must be .momentum.co.za email
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be atleast 8 characters")
  @Pattern(
      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$",
      message =
          "Password must contain atleast one uppercase letter, one number, and one special character (!@#$%^&*)")
  private String password;
}

// password regex from: https://www.baeldung.com/java-regex-password-validation 
