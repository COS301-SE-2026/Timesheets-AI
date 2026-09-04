package timesheets.integration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.auth.GoogleOAuthService;
import timesheets.auth.GoogleTokenResponse;
import timesheets.auth.OAuthState;
import timesheets.auth.OAuthStateService;
import timesheets.domain.IntegrationToken;
import timesheets.dto.response.JiraIssueResponse;
import timesheets.integration.issue.JiraAdapter;
import timesheets.integration.issue.JiraOAuthService;
import timesheets.repository.IntegrationTokenRepository;
import timesheets.security.SecurityUtils;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

  private final OAuthStateService oauthStateService;
  private final GoogleOAuthService googleOAuthService;
  private final SecurityUtils securityUtils;
  private final IntegrationTokenRepository integrationTokenRepository;
  private final JiraOAuthService jiraOAuthService;
  private final JiraAdapter jiraAdapter;

  @GetMapping("/google/calendar/connect")
  public ResponseEntity<String> connectGoogleCalender() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    String state = oauthStateService.generateState(workspaceMemberId, "GOOGLE_CALENDAR");
    String authorizationUrl = googleOAuthService.buildAuthorizationUrl(state);
    return ResponseEntity.ok(authorizationUrl);
  }

  /* callback endpoint
  Where: Google sent the result
   this is when we:
    - receive code (code is temp authorization token)
    - validate state  (what we earlier generated)
    - exhange code tokens
    This is to ensure that the OAuth flow belongs to correct workspace member
  */

  /*
     This prevents hacker from intercept the Google callback and changing
     the state string to link Google Calendar to different workspaceMemberId
  */
  @GetMapping("/google/calendar/callback")
  public ResponseEntity<String> googleCalendarCallBack(
      @RequestParam String code, @RequestParam String state) {

    OAuthState validatedState = oauthStateService.validateState(state);

    // Get the workspace member ID from the validated state
    // this is the member who started the OAuth connection
    UUID workspaceMemberId = validatedState.getWorkspaceMemberId();

    // get the Google code and exchange it for tokens (accessToken, refreshToken, expiresIn) and I
    // will store these in the integrations_token table
    // Exchange Google's authorization code for tokens
    GoogleTokenResponse tokenResponse = googleOAuthService.exchangeCodeforToken(code);

    // calculate when access token expires
    // why? Google access token is temporary
    // if it expires: use refresh token (this help the backend to get new access token w/o the user
    // connecting back to Google Calendar)
    // if not: use existing access token
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());

    // using the Repository class, check if the workspace memer has already connected to Google
    // Calendar
    Optional<IntegrationToken> existingToken =
        integrationTokenRepository.findByWorkspaceMemberIdAndProvider(
            workspaceMemberId, "GOOGLE_CALENDAR");

    // checking if the member has integration token
    // yes: use existing one
    // no: create one
    // for both cases: save the token information

    IntegrationToken integrationToken;
    if (existingToken.isEmpty()) {
      integrationToken = new IntegrationToken();
    } else {
      integrationToken = existingToken.get();
    }

    integrationToken.setWorkspaceMemberId(workspaceMemberId);
    integrationToken.setProvider("GOOGLE_CALENDAR");
    integrationToken.setAccessToken(tokenResponse.getAccessToken());
    integrationToken.setExpiresAt(expiresAt);

    // replace refresh token if Google provide one
    if (tokenResponse.getRefreshToken() != null) {
      integrationToken.setRefreshToken(tokenResponse.getRefreshToken());
    }

    // store in the integration_table
    integrationTokenRepository.save(integrationToken);

    // return ResponseEntity.ok(
    //     "Google Calendar connected for the workspace member: "
    //         + validatedState.getWorkspaceMemberId());

    // ensures that the redirect! forgot to redirect to frontend
    // cleo need to redirect it to the calendar page
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, "http://localhost:4200")
        .build();
  }

  @GetMapping("/jira/connect")
  public ResponseEntity<String> connectJira() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    String state = oauthStateService.generateState(workspaceMemberId, "JIRA");

    String authorizationUrl = jiraOAuthService.buildAuthorizationUrl(state);

    return ResponseEntity.ok(authorizationUrl);
  }

  @GetMapping("/jira/callback")
  public ResponseEntity<String> jiraCallback(
      @RequestParam String code, @RequestParam String state) {

    // validare the state
    OAuthState validatedState = oauthStateService.validateState(state);

    // verify the users are the one that started OAuth flow
    UUID workspaceMemberId = validatedState.getWorkspaceMemberId();

    JiraOAuthService.JiraTokenResponse tokenResponse = jiraOAuthService.exchangeCode(code);

    // get cloud id associated with the token
    String cloudId = jiraOAuthService.getCloudID(tokenResponse.getAccessToken());

    // calculate when the access token expires
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());

    Optional<IntegrationToken> existingToken =
        integrationTokenRepository.findByWorkspaceMemberIdAndProvider(workspaceMemberId, "JIRA");

    // Use existing token or create a new one
    IntegrationToken integrationToken;

    if (existingToken.isEmpty()) {
      integrationToken = new IntegrationToken();
    } else {
      integrationToken = existingToken.get();
    }

    integrationToken.setWorkspaceMemberId(workspaceMemberId);
    integrationToken.setProvider("JIRA");
    integrationToken.setProviderResourceId(cloudId);
    integrationToken.setAccessToken(tokenResponse.getAccessToken());
    integrationToken.setExpiresAt(expiresAt);

    // Jira provides a refresh token
    if (tokenResponse.getRefreshToken() != null) {
      integrationToken.setRefreshToken(tokenResponse.getRefreshToken());
    }

    integrationTokenRepository.save(integrationToken);

    // return ResponseEntity.ok("Jira connected for the workspace member:" + workspaceMemberId);

    // cleo need to redirect it to the calendar page
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, "http://localhost:4200/my-tasks")
        .build();
  }

  // this will get all the current issues for the user
  @GetMapping("/jira/issues")
  public ResponseEntity<List<JiraIssueResponse>> getJiraIssues() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    List<JiraIssueResponse> issues = jiraAdapter.getIssues(workspaceMemberId);
    return ResponseEntity.ok(issues);
  }

  // this will get a specific Jira issue by it's key
  @GetMapping("/jira/issues/{issueKey}")
  public ResponseEntity<JiraIssueResponse> getJiraIssue(@PathVariable String issueKey) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    JiraIssueResponse issue = jiraAdapter.getIssue(workspaceMemberId, issueKey);
    return ResponseEntity.ok(issue);
  }
}
