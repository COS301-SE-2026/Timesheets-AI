package timesheets.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleTokenResponse {
  // jackson used to map the java variables to json reponse

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("refresh_token")
  private String refreshToken;

  // no need to convert because it is written the same as Google 
  private String scope;

  @JsonProperty("token_type")
  private String tokenType;
}
