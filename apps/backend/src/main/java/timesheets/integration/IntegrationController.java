package timesheets.integration;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.integration.auth.GoogleOAuthService;
import timesheets.integration.auth.OAuthStateService;
import timesheets.security.SecurityUtils;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

  private final OAuthStateService oauthStateService;
  private final GoogleOAuthService googleOAuthService;
  private final SecurityUtils securityUtils;

  @GetMapping("/google/calendar/connect")
  public ResponseEntity<String> connectGoogleCalender() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    String state = oauthStateService.generateState(workspaceMemberId, "GOOGLE_CALENDER");
    String authorizationUrl = googleOAuthService.buildAuthorizationUrl(state);
    return ResponseEntity.ok(authorizationUrl);
  }
}
