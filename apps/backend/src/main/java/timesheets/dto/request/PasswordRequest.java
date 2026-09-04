package timesheets.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// I am making one file that handles everything that has to do with password requests

public class PasswordRequest {

  // this will be for when a user forgets their password
  @Data
  public static class Forgot {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
  }

  // the user will submit the token from their email and their new password
  @Data
  public static class Reset {
    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
        message =
            "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String newPassword;
  }

  @Data
  public static class Change {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
        message =
            "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
  }
}
