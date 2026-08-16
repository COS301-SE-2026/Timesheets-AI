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

  @Value("${app.google.client-id}")
  private String clientId;

  @Value("${app.google.client-secret}")
  private String clientSecret;

  @Value("${app.google.redirect-uri}")
  private String redirectUri;
}
