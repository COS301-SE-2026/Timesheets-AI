package timesheets.service.strategy;

import exception.AuthException;
import exception.AuthException.ErrorCode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

// this is the microsoft concrete strategy
@Component
public class MicrosoftSsoStrategy implements SsoAuthenticationStrategy {

  private static final String MICROSOFT_JWK_SET_URI =
      "https://login.microsoftonline.com/common/discovery/v2.0/keys";

  @Value("${app.microsoft.sso.client-id}")
  private String microsoftClientId;

  @Override
  public String getProvider() {
    return "MICROSOFT";
  }

  @Override
  public SsoUserInfo authenticate(String idToken) {

    Jwt token = verifyToken(idToken);

    String tenantId = token.getClaimAsString("tid");
    String subject = token.getSubject();

    if (tenantId == null || tenantId.isBlank() || subject == null || subject.isBlank()) {

      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }

    // microsoft subjects are tenant-specific so combininng both so it works with many tenants
    String providerUserId = tenantId + ":" + subject;

    String email = getEmail(token);
    String[] name = getName(token);

    return new SsoUserInfo(getProvider(), providerUserId, email, name[0], name[1], null, true);
  }

  /*
  - checks that an id token was signed with Microsoft and issued
  - this is so that we know before the id claims are trusted
  */
  private Jwt verifyToken(String idToken) {

    try {
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(MICROSOFT_JWK_SET_URI).build();
      OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();
      OAuth2TokenValidator<Jwt> microsoftValidator =
          token -> {
            String tenantId = token.getClaimAsString("tid");
            String issuer = token.getIssuer() != null ? token.getIssuer().toString() : null;

            // token needs to be issued for timesheets application
            if (!token.getAudience().contains(microsoftClientId)) {
              return validationFailure("Invalid Microsoft token audience");
            }

            if (tenantId == null || tenantId.isBlank()) {
              return validationFailure("Microsoft token does not contain a tenant ID");
            }

            try {
              UUID.fromString(tenantId);
            } catch (IllegalArgumentException exception) {
              return validationFailure("Invalid Microsoft tenant ID");
            }

            String expectedIssuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";

            if (!expectedIssuer.equals(issuer)) {
              return validationFailure("Invalid Microsoft token issuer");
            }

            return OAuth2TokenValidatorResult.success();
          };

      decoder.setJwtValidator(
          new DelegatingOAuth2TokenValidator<>(defaultValidator, microsoftValidator));
      return decoder.decode(idToken);

    } catch (Exception exception) {
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }
  }

  private String getEmail(Jwt token) {

    String email = token.getClaimAsString("email");

    if (email != null && !email.isBlank()) {
      return email;
    }

    String preferredUsername = token.getClaimAsString("preferred_username");

    if (preferredUsername != null && preferredUsername.contains("@")) {
      return preferredUsername;
    }

    return null;
  }

  // this will be to get the users first and last name from the microsoft id token
  private String[] getName(Jwt token) {

    /*
    - this uses the seperate first and last name when Microsoft gives both */
    String firstName = token.getClaimAsString("given_name");
    String lastName = token.getClaimAsString("family_name");

    if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
      return new String[] {firstName.trim(), lastName.trim()};
    }

    // some Microsoft accounts give both, so sometimes we have to split them ourselves
    String displayName = token.getClaimAsString("name");

    if (displayName != null && !displayName.isBlank()) {
      String[] parts = displayName.trim().split("\\s+", 2);

      // when the display name has two parts then it can be mapped
      if (parts.length == 2) {
        return new String[] {parts[0], parts[1]};
      }

      // if there is only one part that will be used as the first name
      // I don't want it rejecting accounts that could be valid just because of this one thing
      return new String[] {parts[0], ""};
    }

    // if there is no valid email provided then there will be a fallback instead of failing
    // everything
    return new String[] {"Microsoft User", ""};
  }

  // this will make a uniform failed validation result
  private OAuth2TokenValidatorResult validationFailure(String message) {
    return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", message, null));
  }
}
