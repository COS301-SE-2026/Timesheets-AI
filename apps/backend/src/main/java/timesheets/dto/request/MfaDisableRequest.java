// Author: Cleopatra Kwenda
// Date: 2026-09-21
// Purpose: Disabling MFA requires the user to
// confirm that they want to diasable it.

package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaDisableRequest {
  @NotBlank(message = "Password is required")
  private String password;
}
