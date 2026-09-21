package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaLoginVerifyRequest {
    @NotBlank(message = "MFA challenge token is required")
    private String challengeToken;

    @NotBlank(message = "TOTP code is required")
    private String totpCode;
}
