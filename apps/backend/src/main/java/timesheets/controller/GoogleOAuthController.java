package timesheets.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import timesheets.auth.GoogleOAuthService;

@RestController
@RequiredArgsConstructor
public class GoogleOAuthController {

  private final GoogleOAuthService googleOAuthService;
}
