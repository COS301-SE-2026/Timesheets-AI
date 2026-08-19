package timesheets.integration.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleTokenResponse {
  private String accessToken;
  private Long expiresIn;
  private String refreshToken;
  private String scope;
  private String tokenType;
}
