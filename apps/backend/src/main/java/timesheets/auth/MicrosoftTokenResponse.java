package timesheets.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//will be the OAuth token response from Microsoft
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MicrosoftTokenResponse {

  //will be used to authenticate the microsoft graph
  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("token_type")
  private String tokenType;

  private String scope;

  //this will have info about the authenticated user
  @JsonProperty("id_token")
  private String idToken;
}
