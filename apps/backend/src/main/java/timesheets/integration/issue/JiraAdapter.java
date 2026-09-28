package timesheets.integration.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import timesheets.domain.Project;
import timesheets.domain.Task;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.CreateIssueRequest;
import timesheets.dto.response.CommentResponse;
import timesheets.dto.response.IssueResponse;
import timesheets.dto.response.StatusChangeResponse;
import timesheets.dto.response.WorklogResponse;
import timesheets.repository.IntegrationTokenRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.WorkspaceMemberRepository;

@Service
@Slf4j
public class JiraAdapter implements IssueTrackerAdapter {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final IntegrationTokenRepository integrationTokenRepository;
  private final TaskRepository taskRepository;
  private final JiraOAuthService jiraOAuthService;
  private final ProjectRepository projectRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  public JiraAdapter(
      IntegrationTokenRepository integrationTokenRepository,
      TaskRepository taskRepository,
      JiraOAuthService jiraOAuthService,
      ProjectRepository projectRepository,
      WorkspaceMemberRepository workspaceMemberRepository) {
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
    this.integrationTokenRepository = integrationTokenRepository;
    this.taskRepository = taskRepository;
    this.jiraOAuthService = jiraOAuthService;
    this.projectRepository = projectRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
  }

  @Override
  public String getProvider() {
    return "JIRA";
  }

  @Override
  public List<IssueResponse> getIssues(UUID workspaceMemberId) {
    IntegrationToken integrationToken = getValidToken(workspaceMemberId);

    String cloudId = integrationToken.getProviderResourceId();

    String url =
        "https://api.atlassian.com/ex/jira/"
            + cloudId
            + "/rest/api/3/search/jql"
            + "?jql=assignee=currentUser()"
            + "&maxResults=50"
            + "&fields=summary,status,issuetype,description,priority,project,assignee,created,updated,duedate";

    HttpHeaders headers = createAuthHeaders(integrationToken.getAccessToken());
    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, request, String.class);

      JsonNode root = objectMapper.readTree(response.getBody());
      JsonNode issues = root.get("issues");

      List<IssueResponse> result = new ArrayList<IssueResponse>();

      if (issues != null && issues.isArray()) {
        for (JsonNode issue : issues) {
          result.add(parseJiraIssue(issue, workspaceMemberId));
        }
      }
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to parse Jira issues", e);
    }
  }

  @Override
  public List<IssueResponse> getIssues(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    List<IssueResponse> issues = getIssues(workspaceMemberId);

    for (IssueResponse issue : issues) {
    List<IssueResponse> filteredIssues = new ArrayList<>();

    for (IssueResponse issue : issues) {
      LocalDateTime createdAt = parseJiraTimestamp(issue.getCreatedAt());
      LocalDateTime updatedAt = parseJiraTimestamp(issue.getUpdatedAt());

      boolean createdInRange =
          createdAt != null && !createdAt.isBefore(startTime) && !createdAt.isAfter(endTime);

      boolean updatedInRange =
          updatedAt != null && !updatedAt.isBefore(startTime) && !updatedAt.isAfter(endTime);

      if (createdInRange || updatedInRange) {
        filteredIssues.add(issue);
      }
    }
    return filteredIssues;
  }

  @Override
  public IssueResponse getIssue(UUID workspaceMemberId, String issueKey) {

    IntegrationToken token = getValidToken(workspaceMemberId);

    String cloudId = token.getProviderResourceId();

    String url = "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/issue/" + issueKey;

    HttpHeaders headers = createAuthHeaders(token.getAccessToken());
    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, request, String.class);

      JsonNode issue = objectMapper.readTree(response.getBody());

      return parseJiraIssue(issue, workspaceMemberId);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to fetch Jira issue: " + issueKey, e);
    }
  }

  @Override
  public IssueResponse createIssue(UUID workspaceMemberId, CreateIssueRequest request) {
    IntegrationToken token = getValidToken(workspaceMemberId);
    String cloudId = token.getProviderResourceId();

    String url = "https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/issue";

    HttpHeaders headers = createAuthHeaders(token.getAccessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    String payload = buildCreatePayload(request);

    HttpEntity<String> httpRequest = new HttpEntity<>(payload, headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, String.class);

      JsonNode result = objectMapper.readTree(response.getBody());
      String issueKey = result.get("key").asText();

      return getIssue(workspaceMemberId, issueKey);

    } catch (Exception e) {
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
    task.setUpdatedAt(LocalDateTime.now());
    taskRepository.save(task);
  }

  @Override
  public List<WorklogResponse> getWorklogs(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    List<IssueResponse> issues = getIssues(workspaceMemberId);
    List<WorklogResponse> worklogs = new ArrayList<>();

    IntegrationToken token = getValidToken(workspaceMemberId);
    String cloudId = token.getProviderResourceId();

    for (IssueResponse issue : issues) {
      String url =
          "https://api.atlassian.com/ex/jira/"
              + cloudId
              + "/rest/api/3/issue/"
              + issue.getKey()
              + "/worklog";

      HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders(token.getAccessToken()));

      try {
        ResponseEntity<String> response =
            restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());

        JsonNode worklogNodes = root.get("worklogs");

        if (worklogNodes == null || !worklogNodes.isArray()) {
          continue;
        }

        for (JsonNode worklogNode : worklogNodes) {
          String started = getString(worklogNode, "started");
          LocalDateTime startedAt = parseJiraTimestamp(started);

          if (startedAt == null) {
            continue;
          }

          boolean withinRange = !startedAt.isBefore(startTime) && !startedAt.isAfter(endTime);

          if (!withinRange) {
            continue;
          }

          WorklogResponse worklog = new WorklogResponse();
          worklog.setIssueKey(issue.getKey());
          worklog.setWorklogId(getString(worklogNode, "id"));
          worklog.setStartedAt(startedAt);
          worklog.setTimeSpentSeconds(
              worklogNode.has("timeSpentSeconds")
                  ? worklogNode.get("timeSpentSeconds").asInt()
                  : 0);
          worklog.setDescription(getString(worklogNode, "comment"));

          JsonNode author = worklogNode.get("author");
          if (author != null && !author.isNull()) {
            worklog.setAuthorDisplayName(getString(author, "displayName"));
            worklog.setAuthorEmail(getString(author, "emailAddress"));
          }
          worklogs.add(worklog);
        }
      } catch (Exception e) {
        log.error("Failed to parse Jira worklogs for the issue: " + issue.getKey(), e);
      }
    }
    return worklogs;
  }

  @Override
  public List<CommentResponse> getComments(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    List<IssueResponse> issues = getIssues(workspaceMemberId);
    List<CommentResponse> comments = new ArrayList<>();
    IntegrationToken token = getValidToken(workspaceMemberId);
    String cloudId = token.getProviderResourceId();

    for (IssueResponse issue : issues) {
      String url =
          "https://api.atlassian.com/ex/jira/"
              + cloudId
              + "/rest/api/3/issue/"
              + issue.getKey()
              + "/comment";

      HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders(token.getAccessToken()));

      try {
        ResponseEntity<String> response =
            restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode commentNodes = root.get("comments");

        if (commentNodes == null || !commentNodes.isArray()) {
          continue;
        }

        for (JsonNode commentNode : commentNodes) {
          String created = getString(commentNode, "created");
          LocalDateTime createdAt = parseJiraTimestamp(created);

          if (createdAt == null) {
            continue;
          }

          boolean withinRange = !createdAt.isBefore(startTime) && !createdAt.isAfter(endTime);

          if (!withinRange) {
            continue;
          }

          CommentResponse comment = new CommentResponse();
          comment.setIssueKey(issue.getKey());
          comment.setCommentId(getString(commentNode, "id"));
          comment.setUpdatedAt(parseJiraTimestamp(getString(commentNode, "updated")));
          comment.setBody(getString(commentNode, "body"));

          JsonNode author = commentNode.get("author");
          if (author != null && !author.isNull()) {
            comment.setAuthorDisplayName(getString(author, "displayName"));
            comment.setAuthorEmail(getString(author, "emailAddress"));
          }

          comments.add(comment);
        }
      } catch (Exception e) {
        log.error("Failed to parse the comments for this issue " + issue.getKey(), e);
      }
    }

    return comments;
  }

  @Override
  public List<StatusChangeResponse> getStatusChanges(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    List<IssueResponse> issues = getIssues(workspaceMemberId);
    List<StatusChangeResponse> statusChanges = new ArrayList<>();
    IntegrationToken token = getValidToken(workspaceMemberId);
    String cloudId = token.getProviderResourceId();

    for (IssueResponse issue : issues) {
      String url =
          "https://api.atlassian.com/ex/jira/"
              + cloudId
              + "/rest/api/3/issue/"
              + issue.getKey()
              + "/changelog"
              + "?startAt=0"
              + "&maxResults=100";

      HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders(token.getAccessToken()));

      try {
        ResponseEntity<String> response =
            restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode histories = root.get("values");

        if (histories == null || !histories.isArray()) {
          continue;
        }

        for (JsonNode history : histories) {
          LocalDateTime changedAt = parseJiraTimestamp(getString(history, "created"));

          if (changedAt == null) {
            continue;
          }

          boolean withinRange = !changedAt.isBefore(startTime) && !changedAt.isAfter(endTime);

          if (!withinRange) {
            continue;
          }

          JsonNode items = history.get("items");
          if (items == null || !items.isArray()) {
            continue;
          }

          for (JsonNode item : items) {
            String field = getString(item, "field");

            if (!"status".equalsIgnoreCase(field)) {
              continue;
            }

            StatusChangeResponse statusChange = new StatusChangeResponse();
            statusChange.setIssueKey(issue.getKey());
            statusChange.setChangeLogId(getString(history, "id"));
            statusChange.setChangedAt(changedAt);
            statusChange.setFromStatus(getString(item, "fromString"));
            statusChange.setToStatus(getString(item, "toString"));

            JsonNode author = history.get("author");
            if (author != null && !author.isNull()) {
              statusChange.setAuthorDisplayName(getString(author, "displayName"));
              statusChange.setAuthorEmail(getString(author, "emailAddress"));
            }

            statusChanges.add(statusChange);
          }
        }
      } catch (Exception e) {
        log.error("Failed to parse the Jira status changes for this issue:" + issue.getKey(), e);
      }
    }

    return statusChanges;
  }

  // Helper functions
  private HttpHeaders createAuthHeaders(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private IssueResponse parseJiraIssue(JsonNode issue, UUID workspaceMemberId) {
    String key = issue.get("key").asText();
    JsonNode fields = issue.get("fields");

    IssueResponse dto = new IssueResponse();
    dto.setKey(key);
    dto.setTitle(getString(fields, "summary"));
    dto.setStatus(getNestedString(fields, "status", "name"));
    dto.setIssueType(getNestedString(fields, "issuetype", "name"));
    dto.setDescription(getString(fields, "description"));
    dto.setPriority(getNestedString(fields, "priority", "name"));
    dto.setProjectKey(getNestedString(fields, "project", "key"));
    dto.setProjectName(getNestedString(fields, "project", "name"));

    WorkspaceMember workspaceMember =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new RuntimeException("Workspace member not found"));
    UUID workspaceId = workspaceMember.getWorkspaceId();

    Optional<Project> localProject =
        projectRepository.findByWorkspaceIdAndJiraProjectKey(workspaceId, dto.getProjectKey());

    if (localProject.isPresent()) {
      dto.setLocalProjectId(localProject.get().getId());
    }
    dto.setCreatedAt(getString(fields, "created"));
    dto.setUpdatedAt(getString(fields, "updated"));
    dto.setDueDate(getString(fields, "duedate"));

    if (fields.has("assignee") && !fields.get("assignee").isNull()) {
      JsonNode assignee = fields.get("assignee");
      dto.setAssigneeEmail(getString(assignee, "emailAddress"));
      dto.setAssigneeDisplayName(getString(assignee, "displayName"));
    }

    Optional<Task> localTask = taskRepository.findByJiraTicketKey(key);

    if (localTask.isPresent()) {
      dto.setLocalTaskId(localTask.get().getId());
      dto.setLocalProjectId(localTask.get().getProjectId());
    }

    return dto;
  }

  private LocalDateTime parseJiraTimestamp(String timestamp) {
    if (timestamp == null || timestamp.isBlank()) {
      return null;
    }

    try {
      return OffsetDateTime.parse(timestamp).toLocalDateTime();
    } catch (Exception e) {
      try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return OffsetDateTime.parse(timestamp, formatter).toLocalDateTime();
      } catch (Exception secondException) {
        log.warn("Could not parse Jira timestamp: {}", timestamp);
        return null;
      }
    }
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

  private String buildCreatePayload(CreateIssueRequest request) {
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
        + escapeJson(request.getTitle())
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

  private IntegrationToken getValidToken(UUID workspaceMemberId) {
    IntegrationToken token =
        integrationTokenRepository
            .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "JIRA")
            .orElseThrow(() -> new RuntimeException("Jira is not connected for this user"));

    if (token.getExpiresAt() != null
        && token.getExpiresAt().minusMinutes(5).isBefore(LocalDateTime.now())) {

      log.info("Jira token expired or about to expire, refreshing...");

      try {
        JiraOAuthService.JiraTokenResponse newToken =
            jiraOAuthService.refreshAccessToken(token.getRefreshToken());

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
