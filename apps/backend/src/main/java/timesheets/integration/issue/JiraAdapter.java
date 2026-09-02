package timesheets.integration.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import timesheets.domain.IntegrationToken;
import timesheets.dto.IssueDto;
import timesheets.repository.IntegrationTokenRepository;

/*
this class talks to Jira's API on behalf of your application
it will take the Jira OAuth token stored in the db, call Jira and convert Jira's response
into your application's IssueDto objects

ObjectMapper converts the JSON string into a tree of JsonNode objects

*/
@Service
public class JiraAdapter implements IssueTrackerAdapter {

  // used to communicate with Jira
  private final RestTemplate restTemplate;

  // used to parse the response
  private final ObjectMapper objectMapper;

  // used to find the OAuth token beloning to the member
  private final IntegrationTokenRepository integrationTokenRepository;

  public JiraAdapter(IntegrationTokenRepository integrationTokenRepository) {
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
    this.integrationTokenRepository = integrationTokenRepository;
  }

  @Override
  public String getProvider() {
    return "JIRA";
  }

  // get all Jira issues associated with the member
  @Override
  public List<IssueDto> getIssues(UUID workspaceMemberId) {
    // find the Jira integration token  for this workspace member
    IntegrationToken integrationToken =
        integrationTokenRepository
            .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "JIRA")
            .orElseThrow(() -> new RuntimeException("Jira is not connected."));

    String cloudId = integrationToken.getProviderResourceId();

    String url =
        "https://api.atlassian.com/ex/jira/"
            + cloudId
            + "/rest/api/3/search/jql"
            + "?jql=assignee%3DcurrentUser()"
            + "&maxResults=50"
            + "&fields=summary,status,issuetype";

    // add OAuth access token
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(integrationToken.getAccessToken());

    HttpEntity<Void> request = new HttpEntity<Void>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.GET, request, String.class);

    // the parsing occurs here

    try {
      JsonNode root = objectMapper.readTree(response.getBody());
      JsonNode issues = root.get("issues");
      List<IssueDto> result = new ArrayList<IssueDto>();

      // issues must be a array to avoid unexpected JSON
      if (issues != null && issues.isArray()) {
        for (JsonNode issue : issues) {
          String key = issue.get("key").asText();
          JsonNode fields = issue.get("fields");
          String summary = fields.get("summary").asText();
          String status = fields.get("status").get("name").asText();
          String issueType = fields.get("issuetype").get("name").asText();

          // convert the jira issue into DTO
          result.add(new IssueDto(key, summary, status, issueType));
        }
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Jira issues", e);
    }
  }

  @Override
  public IssueDto getIssue(UUID workspaceMemberId, String issueKey) {
    return null;
  }
}
