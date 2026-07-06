package timesheets.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// conatins the info for MFA setup, that is returned after 2FA is initiated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupResponse {
  private String secretKey; // this is what the authenticator app uses to generate the TOTP
  private String
      qrCodeUrl; // I want this to contain a link to generating URL's with authenticator apps
  private String message;
}
