/*
This file handles the github oauth connect/callback flow, mirrows IntegrationControllers, google calendar handling, sync added will be added later once service exists

Author: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.integration.github;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.auth.OAuthState;
import timesheets.auth.OAuthStateService;
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
  public ResponseEntity<String> connect() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    String state = oauthStateService.generateState(workspaceMemberId, "GITHUB");
    String authorizationUrl = gitHubOAuthService.buildAuthorisationUrl(state);
    return ResponseEntity.ok(authorizationUrl);
  }

  // this prevents someone from intecepting the github call back and linking fithub to a different
  // workspace member

  @GetMapping("/callback")
  public ResponseEntity<String> callback(@RequestParam String code, @RequestParam String state) {

    OAuthState validatedState = oauthStateService.validateState(state);
    UUID workspaceMemberId = validatedState.getWorkspaceMemberId();

    gitHubService.exchangeAndsaveToken(workspaceMemberId, code);

    return ResponseEntity.ok("GitHub connected for workspace member: " + workspaceMemberId);
  }

  @PostMapping("/sync")
  public ResponseEntity<Integer> sync() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    int synced = gitHubService.syncRecentCommits(workspaceMemberId);
    return ResponseEntity.ok(synced);
  }
}
