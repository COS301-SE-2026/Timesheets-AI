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
import org.springframework.web.bind.annotation.RequestParam;

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

  /* callback endpoint
  Where: Google sent the result 
   this is when we:
    - receive code (code is temp authorization token)
    - validate state  (what we earlier generated)
    - exhange code tokens 
    This is to ensure that the OAuth flow belongs to correct workspace member 
  */

    @GetMapping("/google/calender/callback")
    public ResponseEntity<String> googleCalenderCallBack(@RequestParam String code, @RequestParam String state){
      // TODO 
      return ResponseEntity.ok("Activated callback");
    }
  
}
