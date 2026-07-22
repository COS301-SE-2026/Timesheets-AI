package timesheets.dto.request;

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
  @Pattern(
      regexp =
          "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@(momentum\\.co\\.za|momentum\\.com)$",
      message =
          "Email must be from the accepted domain") // should be momentum.co.za or momentum.com
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be atleast 8 characters")
  @Pattern(
      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
      message =
          "Password must contain atleast one uppercase letter, one number, and one special character (!@#$%^&*)")
  private String password;
}

// password regex from: https://www.baeldung.com/java-regex-password-validation
// email regex from: https://www.baeldung.com/java-email-validation-regex
