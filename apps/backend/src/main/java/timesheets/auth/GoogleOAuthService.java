// this will authorise, exhange authorization code and refresh access token
// endpoint to introduce is GET /api/integrations/google/calendar/authorize
// frontend calls it when user click Connect Google Calender
// backend sends the user to Google's authorization page
// After Googe grants permission, Google redirects back to backend
// Google returns an authorization code
// Spring Boot will exhage code for access and refresh tokens
// link: https://developers.google.com/identity/protocols/oauth2/web-server

package timesheets.integration.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleOAuthService {

  // GOOGLE's primary OAUTH 2.0 endpoint used to authencitate users and request authorization
  private static final String GOOGLE_AUTHORIZATION_URL =
      "https://accounts.google.com/o/oauth2/v2/auth";

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
}
