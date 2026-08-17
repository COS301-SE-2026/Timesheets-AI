package timesheets.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.integration.auth.GoogleOAuthService;
import timesheets.integration.auth.OAuthStateService;
import java.util.UUID;


@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

  private final OAuthStateService oauthStateService;
  private final GoogleOAuthService googleOAuthService;
  
}
