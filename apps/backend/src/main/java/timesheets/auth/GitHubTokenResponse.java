/*
This file handles the jackson maps straight off the github token exchange response

Author: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitHubTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("token_type")
  private String tokenType;

  private String scope;

  // githubs classic oauth apps dont return these by default, only github apps get token expiry.
  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("expires_in")
  private Long expiresIn;
}
