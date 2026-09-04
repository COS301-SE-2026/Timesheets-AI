package timesheets.integration.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import timesheets.domain.IntegrationToken;
import timesheets.domain.Task;
import timesheets.dto.request.CreateJiraIssueRequest;
import timesheets.dto.response.JiraIssueResponse;
import timesheets.repository.IntegrationTokenRepository;
import timesheets.repository.TaskRepository;

/*
this class talks to Jira's API on behalf of your application
it will take the Jira OAuth token stored in the db, call Jira and convert Jira's response
into your application's IssueDto objects

ObjectMapper converts the JSON string into a tree of JsonNode objects

*/
@Service
@Slf4j
public class JiraAdapter implements IssueTrackerAdapter {

  // used to communicate with Jira
  private final RestTemplate restTemplate;

  // used to parse the response
  private final ObjectMapper objectMapper;

  // used to find the OAuth token beloning to the member
  private final IntegrationTokenRepository integrationTokenRepository;

  private final TaskRepository taskRepository;
  private final JiraOAuthService jiraOAuthService;

  public JiraAdapter(
      IntegrationTokenRepository integrationTokenRepository,
      TaskRepository taskRepository,
      JiraOAuthService jiraOAuthService) {
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
    this.integrationTokenRepository = integrationTokenRepository;
    this.taskRepository = taskRepository;
    this.jiraOAuthService = jiraOAuthService;
  }

  @Override
  public String getProvider() {
    return "JIRA";
  }

  // get all Jira issues associated with the member
  @Override
  public List<JiraIssueResponse> getIssues(UUID workspaceMemberId) {
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
            + "?jql=assignee=currentUser()"
            + "&maxResults=50"
            + "&fields=summary,status,issuetype,description,priority,project,assignee,created,updated,duedate";

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

      List<JiraIssueResponse> result = new ArrayList<JiraIssueResponse>();

      // issues must be a array to avoid unexpected JSON
      if (issues != null && issues.isArray()) {
        for (JsonNode issue : issues) {
          result.add(parseJiraIssue(issue));
        }
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Jira issues", e);
    }
  }

  @Override
  public JiraIssueResponse getIssue(UUID workspaceMemberId, String issueKey) {
    IntegrationToken token = getValidToken(workspaceMemberId);
    String cloudId = token.getProviderResourceId();

    String url = "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/issue/" + issueKey;

    HttpHeaders headers = createAuthHeaders(token.getAccessToken());
    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, request, String.class);

      JsonNode issue = objectMapper.readTree(response.getBody());
      return parseJiraIssue(issue);
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch Jira issue: " + issueKey, e);
    }
  }

  @Override
  public JiraIssueResponse createIssue(UUID workspaceMemberId, CreateJiraIssueRequest request) {

    IntegrationToken token = getValidToken(workspaceMemberId);
    String cloudId = token.getProviderResourceId();

    String url = "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/issue";

    HttpHeaders headers = createAuthHeaders(token.getAccessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    String payload = buildCreatePayload(request);

    HttpEntity<String> httpRequest = new HttpEntity<>(payload, headers);

    try {
      log.info("Creating Jira issue in project: {}", request.getProjectKey());
      log.debug("Jira create issue payload: {}", payload);

      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, httpRequest, String.class);

      log.info("Jira issue creation successful. Status: {}", response.getStatusCode());

      JsonNode result = objectMapper.readTree(response.getBody());

      String issueKey = result.get("key").asText();

      log.info("Created Jira issue: {}", issueKey);

      return getIssue(workspaceMemberId, issueKey);

    } catch (org.springframework.web.client.HttpStatusCodeException e) {

      log.error(
          "Jira API returned error {}: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);

      throw new RuntimeException(
          "Jira API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);

    } catch (Exception e) {

      log.error("Failed to create Jira issue", e);

      throw new RuntimeException("Failed to create Jira issue", e);
    }
  }

  @Override
  public void linkTaskToIssue(UUID workspaceMemberId, UUID taskId, String issueKey) {
    Task task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

    task.setJiraTicketKey(issueKey);
    task.setUpdatedAt(java.time.LocalDateTime.now());
    taskRepository.save(task);

    log.info("Linked task {} to Jira issue: {}", taskId, issueKey);
  }

  // ! helper functions
  private HttpHeaders createAuthHeaders(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private JiraIssueResponse parseJiraIssue(JsonNode issue) {
    String key = issue.get("key").asText();
    JsonNode fields = issue.get("fields");

    JiraIssueResponse dto = new JiraIssueResponse();
    dto.setKey(key);
    dto.setSummary(getString(fields, "summary"));
    dto.setStatus(getNestedString(fields, "status", "name"));
    dto.setIssueType(getNestedString(fields, "issuetype", "name"));
    dto.setDescription(getString(fields, "description"));
    dto.setPriority(getNestedString(fields, "priority", "name"));
    dto.setProjectKey(getNestedString(fields, "project", "key"));
    dto.setProjectName(getNestedString(fields, "project", "name"));
    dto.setCreatedAt(getString(fields, "created"));
    dto.setUpdatedAt(getString(fields, "updated"));
    dto.setDueDate(getString(fields, "duedate"));

    if (fields.has("assignee") && !fields.get("assignee").isNull()) {
      JsonNode assignee = fields.get("assignee");
      dto.setAssigneeEmail(getString(assignee, "emailAddress"));
      dto.setAssigneeDisplayName(getString(assignee, "displayName"));
    }

    return dto;
  }

  private String getString(JsonNode node, String field) {
    return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
  }

  private String getNestedString(JsonNode node, String parent, String child) {
    if (node.has(parent) && !node.get(parent).isNull()) {
      JsonNode parentNode = node.get(parent);
      return parentNode.has(child) && !parentNode.get(child).isNull()
          ? parentNode.get(child).asText()
          : null;
    }
    return null;
  }

  private String buildCreatePayload(CreateJiraIssueRequest request) {
    String description =
        request.getDescription() != null
            ? "{"
                + "\"type\":\"doc\","
                + "\"version\":1,"
                + "\"content\":["
                + "{"
                + "\"type\":\"paragraph\","
                + "\"content\":["
                + "{"
                + "\"type\":\"text\","
                + "\"text\":\""
                + escapeJson(request.getDescription())
                + "\"}"
                + "]"
                + "}"
                + "]"
                + "}"
            : null;

    return "{"
        + "\"fields\":{"
        + "\"project\":{\"key\":\""
        + escapeJson(request.getProjectKey())
        + "\"},"
        + "\"summary\":\""
        + escapeJson(request.getSummary())
        + "\","
        + "\"description\":"
        + (description != null ? description : "null")
        + ","
        + "\"issuetype\":{\"name\":\""
        + escapeJson(request.getIssueType())
        + "\"}"
        + (request.getPriority() != null
            ? ",\"priority\":{\"name\":\"" + escapeJson(request.getPriority()) + "\"}"
            : "")
        + (request.getDueDate() != null
            ? ",\"duedate\":\"" + escapeJson(request.getDueDate()) + "\""
            : "")
        + "}}";
  }

  private String escapeJson(String value) {
    if (value == null) return "";
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }

  //! this method has been failing so I need to look at it again
  private IntegrationToken getValidToken(UUID workspaceMemberId) {
    IntegrationToken token =
        integrationTokenRepository
            .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "JIRA")
            .orElseThrow(() -> new RuntimeException("Jira is not connected for this user"));

    //this will check if the token has expired or expires in 5 mins
    if (token.getExpiresAt() != null
        && token.getExpiresAt().minusMinutes(5).isBefore(LocalDateTime.now())) {

      log.info("Jira token expired or about to expire, refreshing...");

      try {
        JiraOAuthService.JiraTokenResponse newToken =
            jiraOAuthService.refreshAccessToken(token.getRefreshToken());

        //wanting to update the token in the DB so that we always have the most recent updated
        token.setAccessToken(newToken.getAccessToken());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(newToken.getExpiresIn()));

        if (newToken.getRefreshToken() != null) {
          token.setRefreshToken(newToken.getRefreshToken());
        }

        integrationTokenRepository.save(token);
        log.info("Jira token refreshed successfully");

      } catch (Exception e) {
        log.error("Failed to refresh Jira token", e);
        throw new RuntimeException("Jira token expired. Please reconnect your Jira account.", e);
      }
    }

    return token;
  }
}
