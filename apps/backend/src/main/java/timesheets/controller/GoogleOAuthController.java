package timesheets.integration.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import timesheets.integration.auth.GoogleOAuthService;

@RestController
@RequiredArgsConstructor
public class GoogleOAuthController {

  private final GoogleOAuthService googleOAuthService;
}
