package timesheets.integration.issue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class JiraOAuthService {
  private static final String AUTHORIZATION_URL = "https://auth.atlassian.com/authorize";
  private static final String TOKEN_URL = "https://auth.atlassian.com/oauth/token";
  private static final String RESOURCES_URL =
      "https://api.atlassian.com/oauth/token/accessible-resources";

  @Value("${app.jira,client-id}")
  private String clientId;

  @Value("${app.jira,client-secret}")
  private String clientSecret;

  @Value("${app.jira,redirect-uri}")
  private String redirectUri;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectmapper;

  public JiraOAuthService() {
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

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

  
}
