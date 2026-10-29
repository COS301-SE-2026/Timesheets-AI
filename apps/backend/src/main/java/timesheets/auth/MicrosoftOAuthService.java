package timesheets.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

// this will handle the microsoft OAuth flow
@Service
public class MicrosoftOAuthService {

  private static final String MICROSOFT_AUTHORIZATION_URL =
      "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";

  private static final String MICROSOFT_TOKEN_URL =
      "https://login.microsoftonline.com/common/oauth2/v2.0/token";

  @Value("${app.microsoft.sso.client-id}")
  private String clientId;

  @Value("${app.microsoft.sso.client-secret}")
  private String clientSecret;

  @Value("${app.microsoft.calendar.redirect-uri}")
  private String redirectUri;

  private final RestClient restClient = RestClient.create();

  public String buildAuthorizationUrl(String state, String loginHint) {

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(MICROSOFT_AUTHORIZATION_URL)
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_mode", "query")
            .queryParam("scope", "openid profile email offline_access Calendars.Read")
            .queryParam("state", state);

    // this is to ensure we are not assumming that their email address as their Microsoft account -
    // provides flexibility
    if (loginHint != null && !loginHint.isBlank()) {
      builder.queryParam("login_hint", loginHint);
    }

    return builder.build().encode().toUriString();
    // the offline_access is so that microsoft can give a refresh token
  }

  public MicrosoftTokenResponse exchangeCodeForToken(String code) {

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("code", code);
    formData.add("redirect_uri", redirectUri);
    formData.add("grant_type", "authorization_code");
    formData.add("scope", "openid profile email offline_access Calendars.Read");

    // microsoft needs the token request to be sent as form data
    return restClient
        .post()
        .uri(MICROSOFT_TOKEN_URL)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(formData)
        .retrieve()
        .body(MicrosoftTokenResponse.class);
  }

  // adding refresh token method (this is for when the access token is expired)
  public MicrosoftTokenResponse refreshAccessToken(String refreshToken) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("grant_type", "refresh_token");
    formData.add("refresh_token", refreshToken);
    formData.add("scope", "openid profile email offline_access Calendars.Read");

    // microsoft needs the token request to be sent as form data
    return restClient
        .post()
        .uri(MICROSOFT_TOKEN_URL)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(formData)
        .retrieve()
        .body(MicrosoftTokenResponse.class);
  }
}
