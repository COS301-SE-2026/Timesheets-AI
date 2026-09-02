package timesheets.integration.issue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class JiraOAuthService {

  // endpoint where the user is redirected to log in and authorize the application
  private static final String AUTHORIZATION_URL = "https://auth.atlassian.com/authorize";

  // Atlassian endpoint used to exchange an authorization code for an access and refresh token
  private static final String TOKEN_URL = "https://auth.atlassian.com/oauth/token";

  // endpoint to get all Atlassian cloud resources that the authenticated user can access
  private static final String RESOURCES_URL =
      "https://api.atlassian.com/oauth/token/accessible-resources";

  @Value("${app.jira,client-id}")
  private String clientId;

  @Value("${app.jira,client-secret}")
  private String clientSecret;

  @Value("${app.jira,redirect-uri}")
  private String redirectUri;

  // Used to make HTTP requests to the Atlassian OAuth API
  private final RestTemplate restTemplate;

  // Used to parse JSON responses returned by Atlassian
  private final ObjectMapper objectmapper;

  public JiraOAuthService() {
    // Create the HTTP client used for OAuth requests
    this.restTemplate = new RestTemplate();

    // Create JSON parser
    this.objectMapper = new ObjectMapper();
  }

  // build authorization URL
  // Build the URL that frontend/user is redirected to in order to authorize this application with
  // Jira
  /*
    permissions we are requesting
    read:jira-work --> read Jira issues and work data
    read:jira-user --> read Jira user information
    read:me --> read the authenticated user's identity
  */
  public String buildAuthrizationUrl(String state) {
    return AUTHORIZATION_URL
        + "?audience=api.atlassian.com"
        + "&client_id="
        + clientId
        + "&scope=read%3Ajira-work%20read%3Ajira-user%20read%3Ame"
        + "redirect_uri="
        + encode(redirectUri)
        + "&state="
        + encode(state)
        + "&response_type=code"
        + "&prompt=consent";
  }

  // exchange authorizatiion code for an access token and refresh token
  public JiraTokenResponse exchangeCode(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String credentials = clientId + ":" + clientSecret;

    String encodedCredentials =
        Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

    headers.set("Authorization:", "Basic" + encodedCredentials);

    // Build JSON request body required by the OAuth token endpoint
    String body =
        "{"
            + "\"grant_type\":\"authorization_code\","
            + "\"code\":\""
            + escapeJson(code)
            + "\","
            + "\"redirect_uri\":\""
            + escapeJson(redirectUri)
            + "\""
            + "}";

    HttpEntity<String> request = new HttpEntity<String>(body, headers);

    ResponseEntity<String> response =
        restTemplare.exchange(TOKEN_URL, HttpMethod.POST, request, String.class);

    try {

      JsonNode json = objectMapper.readTree(response.getBody());
      // extract the access token, refresh token and expiry time

      return new JiraTokenResponse(
          json.get("access_token").asText(),
          json.has("refresh_token") ? json.get("refresh_time").asText() : null,
          josn.has("expires_in") ? json.get("expires_in").asLong() : 3600);
    } catch (Exception e) {
      // Throw an application error if the OAuth response cannot be parsed correctly
      throw new RuntimeException("Failed to parse Jira OAuth response", e);
    }
  }

  // Retrieve the Atlassian Cloud ID for Jira site accessible to the authenticated user
  // The Cloud ID is required when making Jira REST API requests through api.atlassian.com

  public String getCloudID(String accessToken) {
    // create HTTP headers for the request
    HttpHeaders headers = new HttpHeaders();

    // add oauth access token using bearer authentication
    headers.setBearerAuth(accessToken);

    // create an HTTP request containing only the headers
    HttpEntity<Void> request = new HttpEntity<Void>(headers);

    // request list of cloud resources available to the authenticated user

    ResponseEntity<String> response =
        restTemplate.exchange(RESOURCES_URL, HttpMethod.GET, request, String.class);

    try {
      // parse the array
      JsonNode resources = objectMapper.readTree(response.getBody());

      // ensure that there is at least one resources found

      if (!resources.isArray() || resources.size() == 0) {
        throw new RuntimeException("No Jira resources are availiable for this account.");
      }

      return resources.get(0).get("id").asText();
    } catch (Exception e) {
      throw new RuntimeException("Failed to retrieve Jira cloud ID", e);
    }
  }

  private String encode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  public static class JiraTokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;

    public JiraTokenResponse(String accessToken, String refreshToken, long expiresIn) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
      return accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public long getExpiresIn() {
      return expiresIn;
    }
  }
}
