/*
This file will handle the authorisation code for access token endpoint to introduce
is GET api/integrations/gitub/connect, frontend calls it when user clicks connect github backend sends the user to Github's authorisation page
after Github grants permission, github will redirect back to backend
Github returns an auth code, backend will exchange the auth code for an access token and store it in the database

Authr: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GitHubOAuthService {
  private static final String GITHUB_AUTHORISATION_URL = "https://github.com/login/oauth/authorize";
  private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";

  @Value("${app.github.client-id}")
  private String clientId;

  @Value("${app.github.client-secret}")
  private String clientSecret;

  @Value("${app.github.redirect-uri}")
  private String redirectUri;

  /*
  our client id identifies our application to GitHub,
  and the redirect URI is where GitHub will send the user after they authorize our application
  */

  public String buildAuthorisationUrl(String state) {
    return UriComponentsBuilder.fromHttpUrl(GITHUB_AUTHORISATION_URL)
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("scope", "repo read:user")
        .queryParam("state", state)
        .build()
        .encode()
        .toUriString();
  }

  // sends POST request to Githubs token endpoint with form data
  private final RestClient restClient = RestClient.create();

  public GitHubTokenResponse exchangeCodeForToken(String code) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", code);
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("redirect_uri", redirectUri);

    return restClient
        .post()
        .uri(GITHUB_TOKEN_URL)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "application/json")
        .body(formData)
        .retrieve()
        .body(GitHubTokenResponse.class);
  }
}
