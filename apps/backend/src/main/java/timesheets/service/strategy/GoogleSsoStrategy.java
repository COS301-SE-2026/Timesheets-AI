package timesheets.service.strategy;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import exception.AuthException;
import exception.AuthException.ErrorCode;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
- the concrete strategy for Google
- it will only know about google, not how to create users or how to access the DB
- was previously in Auth service, but that is not following the pattern correctly
*/
@Component
public class GoogleSsoStrategy implements SsoAuthenticationStrategy {

  @Value("${app.google.sso.client-id}")
  private String googleClientId;

  @Override
  public String getProvider() {
    return "GOOGLE";
  }

  @Override
  public SsoUserInfo authenticate(String idToken) {

    GoogleIdToken.Payload payload = verifyToken(idToken);

    String firstName =
        payload.get("given_name") != null ? (String) payload.get("given_name") : "Google User";

    String lastName =
        payload.get("family_name") != null ? (String) payload.get("family_name") : "Unknown";

    return new SsoUserInfo(
        getProvider(),
        payload.getSubject(),
        payload.getEmail(),
        firstName,
        lastName,
        (String) payload.get("picture"),
        Boolean.TRUE.equals(payload.getEmailVerified()));
  }

  private GoogleIdToken.Payload verifyToken(String idToken) {

    // dev token for testing
    if ("swagger-test".equals(idToken)) {

      GoogleIdToken.Payload payload = new GoogleIdToken.Payload();

      payload.setSubject("google-test-user-123");
      payload.setEmail("thabang.siduke@momentum.co.za");
      payload.put("given_name", "Thabang");
      payload.put("family_name", "Siduke");
      payload.put("picture", "https://www.magnific.com/free-photos-vectors/avatar-logo");

      return payload;
    }

    try {

      GoogleIdTokenVerifier verifier =
          new GoogleIdTokenVerifier.Builder(
                  new NetHttpTransport(), GsonFactory.getDefaultInstance())
              .setAudience(Collections.singletonList(googleClientId))
              .build();

      GoogleIdToken googleToken = verifier.verify(idToken);

      if (googleToken == null) {
        throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
      }
      return googleToken.getPayload();

    } catch (Exception exception) {
      throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }
  }
}
