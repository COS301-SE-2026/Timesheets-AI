/*
This file handles the github oauth connect/callback flow, mirrows IntegrationControllers, google calendar handling, sync added will be added later once service exists

Author: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.integration.github;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.auth.OAuthState;
import timesheets.auth.OAuthStateService;
import timesheets.integration.IntegrationController.IntegrationStatus;
import timesheets.repository.IntegrationTokenRepository;
import timesheets.security.SecurityUtils;

@RestController
@RequestMapping("/api/integrations/github")
@RequiredArgsConstructor
public class GitHubIntegrationController {
  private final OAuthStateService oauthStateService;
  private final GitHubOAuthService gitHubOAuthService;
  private final SecurityUtils securityUtils;
  private final IntegrationTokenRepository integrationTokenRepository;
  private final GitHubService gitHubService;

  @GetMapping("/connect")
  public ResponseEntity<String> connect(@RequestParam(required = false) String returnPath) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    String safeReturnPath =
        (returnPath != null && returnPath.startsWith("/")) ? returnPath : "/insights";
    String state = oauthStateService.generateState(workspaceMemberId, "GITHUB", safeReturnPath);
    String authorizationUrl = gitHubOAuthService.buildAuthorisationUrl(state);
    return ResponseEntity.ok(authorizationUrl);
  }

  @Value("${app.frontend-url}")
  private String frontendUrl;

  // this prevents someone from intecepting the github call back and linking fithub to a different
  // workspace member

  @GetMapping("/callback")
  public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
    OAuthState validatedState = oauthStateService.validateState(state);
    UUID workspaceMemberId = validatedState.getWorkspaceMemberId();

    gitHubService.exchangeAndsaveToken(workspaceMemberId, code);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(frontendUrl + validatedState.getReturnPath() + "?github=connected"))
        .build();
  }

  /*
  i added a read endpoint for the commits already synced into git_commits, delegates to the same GitHubAdapter.getCommits the Evidence Engine uses so there's one query path, not two.
  from/to are optional, defaults to the last 7 days to match syncRecentCommits' window.
  */
  @GetMapping("/commits")
  public ResponseEntity<List<GitCommitActivity>> getCommits(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    LocalDate toDate = (to != null) ? to : LocalDate.now();
    LocalDate fromDate = (from != null) ? from : toDate.minusDays(7);

    if (fromDate.isAfter(toDate)) {
      return ResponseEntity.badRequest().build();
    }

    // inclusive of both endpoints: start of the from-day, end of the to-day
    List<GitCommitActivity> commits =
        gitHubService.getCommits(
            workspaceMemberId, fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));

    return ResponseEntity.ok(commits);
  }

  @PostMapping("/sync")
  public ResponseEntity<Integer> sync() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    int synced = gitHubService.syncRecentCommits(workspaceMemberId);
    return ResponseEntity.ok(synced);
  }

  // it mirrors GET /api/calendar/status so the frontend checks every integration the same way
  @GetMapping("/status")
  public ResponseEntity<IntegrationStatus> getStatus() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    boolean connected = gitHubService.isConnected(workspaceMemberId);
    return ResponseEntity.ok(new IntegrationStatus(connected, connected ? "github" : null));
  }
}
