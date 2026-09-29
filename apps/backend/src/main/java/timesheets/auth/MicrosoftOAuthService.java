package timesheets.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

//this will handle the microsoft OAuth flow
@Service
public class MicrosoftOAuthService {

  private static final String MICROSOFT_AUTHORIZATION_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";

  private static final String MICROSOFT_TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

  @Value("${app.microsoft.sso.client-id}")
  private String clientId;

  @Value("${app.microsoft.sso.client-secret}")
  private String clientSecret;

  @Value("${app.microsoft.calendar.redirect-uri}")
  private String redirectUri;

  private final RestClient restClient = RestClient.create();

  public String buildAuthorizationUrl(String state, String loginHint) {

    
    return UriComponentsBuilder.fromHttpUrl(MICROSOFT_AUTHORIZATION_URL)
        .queryParam("client_id", clientId)
        .queryParam("response_type", "code")
        .queryParam("redirect_uri", redirectUri)
        .queryParam("response_mode", "query")
        .queryParam("scope", "openid profile email offline_access Calendars.Read")
        .queryParam("login_hint", loginHint)
        .queryParam("state", state)
        .build()
        .encode()
        .toUriString();
        //the offline_access is so that microsoft can give a refresh token
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
}
