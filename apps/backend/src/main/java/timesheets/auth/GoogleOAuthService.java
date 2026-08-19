// this will authorise, exchange authorization code and refresh access token
// endpoint to introduce is GET /api/integrations/google/calendar/authorize
// frontend calls it when user click Connect Google Calender
// backend sends the user to Google's authorization page
// After Googe grants permission, Google redirects back to backend
// Google returns an authorization code
// Spring Boot will exchange code for access and refresh tokens
// link: https://developers.google.com/identity/protocols/oauth2/web-server

package timesheets.integration.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class GoogleOAuthService {

  // GOOGLE's primary OAUTH 2.0 endpoint used to authencitate users and request authorization
  private static final String GOOGLE_AUTHORIZATION_URL =
      "https://accounts.google.com/o/oauth2/v2/auth";

  private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

  @Value("${app.google.client-id}")
  private String clientId;

  @Value("${app.google.client-secret}")
  private String clientSecret;

  @Value("${app.google.redirect-uri}")
  private String redirectUri;

  /* client id identifies our application to Google
      redirect_uri tells google where to send user after authorization
      response_type - we want google to return the authorization code
      scope - we want saccess and work with user's calender events
      state - protects OAuth from CSRF attacks
      access_type=offline - obtain the refresh token
  */

  // this is url where the users will be sent to Google Permission screen
  public String buildAuthorizationUrl(String state) {
    return GOOGLE_AUTHORIZATION_URL
        + "?client_id="
        + clientId
        + "&redirect_uri="
        + redirectUri
        + "&response_type=code"
        + "&scope=https://www.googleapis.com/calendar.events"
        + "&access_type=offline"
        + "&state="
        + state;
  }

  /*
    GoogleOAuthService asks for state from OAuthStateService
    OAuthStateService creates signed state
    GoogleOAuthService builds Google URL

    **GoogleOAuthService should not know how OAuth state is generated**
  */

  // sending POST HTTP request to Google OAuth APIwith payload liek code, client_id, client_secret
  private final RestClient restClient = RestClient.create();

  /* 
    For exchange tokens part: OAuth and Web Apps
    HTTP Form Data and HTTP Headers in Spring application
    OAuth 2.0 Token Requests: when GoogleAuthService make a POST request to Google 
    to exchange your authorization code for access tokens. 
    Google expects the data to be formatted as a HTTP Form esp for Google OAuth 20, which what we are using 
  */

 public GoogleTokenResponse exchangeCodeforToken (String code){

    // use this because I want to ensure that hte order of keys and values is preserved 
    // it is way cleaner and it automatically creates the list 
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

    // this was the string Google provided with the redirect URL  when  sending user back to our callback endpoint
    formData.add("code", code);
    // our application identifier
    formData.add("client_id", clientId);
    // callback URL in the console 
    formData.add("redirect_uri", redirectUri);
    // to tell Google which OAuth fk=low we are running ;
    formData.add("grant_type", "authorization_code")

 }
}
