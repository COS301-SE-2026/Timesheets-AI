/*
The system is stateless. There are no sessions to use and store nonce
What happended before?
- wroking on the endpoint, I wanted store the state that has been generated and use that to later on verify that it is the same so we can eventually store it in the integration_tokens table
But we are not using sessions so I need to make Stateless OAuth state

state should have workspace member ID, random value and expiration
- it will be short lived security value
GOAL here: angular connect to google calender, do the normal authentication and GoogleOAuthService generate a state and user see consent screen by Google and user accepts and then
redirect begins here. In this callback, we will have code and state and the GoogleOAuthController will verify state and GoogleOuthService will exchange the code and save it into the table.
to verify state, I will make the state a signed value similar to JwtService.java

Avoiding using the jwtService because I want to keep it separate - the JWT represents that the user has been authenticated to Timesheets-AI and
OAuth state represents that the specific OAuth authorization attempt was intiated by this workspace member.

OAuthStateService will:
 - generate state
 - verify state
*/

// before this function run, we have JWT -> SecurityUtils -> workspaceMemberId
// provider = GOOGLE_CALENDER

// this function is when the Google sends 'state = ...' back to our callback
// function is to verify:
/*
    - the signature is valid
    - the state has not expired
    - the provider is the expected provider
    - the workspace member ID can be recovered
*/

package timesheets.integration.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService {

  @Value("${app.jwt.secret}")
  private String secret;

  // signing key
  // create the key used to sign and verify the OAuth state
  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  // temporary state sent to Google
  // shows that workspace member started this particular OAuth operation
  public String generateState(UUID workspaceMemberId, String provider) {
    return Jwts.builder()
        // add workspace member
        .claim("workspaceMemberId", workspaceMemberId.toString())
        // add provider
        .claim("provider", provider)
        .issuedAt(new Date())
        // expiration time of 10 minutes
        .expiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
        // sign the state so it creates a cryptographic key from app.jwt.secret
        // any changes will make the signature invalid
        .signWith(getSigningKey())
        .compact();
  }

  public OAuthState verifyState(String state) {
    Claims claims =
        Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(state)
            .getPayload();

    UUID workspaceMemberId = UUID.fromString(claims.get("workspaceMemberId", String.class));
    String provider = claims.get("provider", String.class);
    // has workspacememberid and provider 
    return new OAuthState(workspaceMemberId, provider);
  }
}
