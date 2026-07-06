package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;

// so Time-Based One-Time Password is a standard for authenticator apps
// for now the authenticator app we are using is google

public class MfaVerifyRequest {
  @NotBlank(message = "TOTP code is required")
  private String totpCode;
}
