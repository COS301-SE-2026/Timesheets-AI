// package timesheets.service;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import java.nio.charset.StandardCharsets;
// import java.security.Key;
// import java.util.Date;
// import java.util.concurrent.TimeUnit;
// import lombok.RequiredArgsConstructor;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import timesheets.domain.User;

// // Handles all JWT token operations: generation, validation and claim extraction.
// // Tokens are signed with a secret key from application.yml and contain the user's
// // email and id as claims so we can identify them on subsequent requests.
// @Service
// @RequiredArgsConstructor
// public class JwtService {

//   @Value("${app.jwt.secret}")
//   private String secret;

//   // generates a signed JWT token for the given user valid for the specified number of days
//   public String generateToken(User user, int expirationDays) {
//     long expirationMillis = TimeUnit.DAYS.toMillis(expirationDays);
//     return Jwts.builder()
//         .subject(user.getEmail())
//         .claim("userId", user.getId().toString())
//         .issuedAt(new Date())
//         .expiration(new Date(System.currentTimeMillis() + expirationMillis))
//         .signWith(getSigningKey())
//         .compact();
//   }

//   // extracts the email (subject) from a token
//   public String extractEmail(String token) {
//     return extractClaims(token).getSubject();
//   }

//   // checks if the token has expired
//   public boolean isTokenExpired(String token) {
//     return extractClaims(token).getExpiration().before(new Date());
//   }

//   // validates the token by checking the email matches and it has not expired
//   public boolean isTokenValid(String token, String email) {
//     return extractEmail(token).equals(email) && !isTokenExpired(token);
//   }

//   // returns the expiration date of the token, used when blacklisting on logout
//   public Date getExpiration(String token) {
//     return extractClaims(token).getExpiration();
//   }

//   private Claims extractClaims(String token) {
//     return Jwts.parser()
//         .verifyWith((javax.crypto.SecretKey) getSigningKey())
//         .build()
//         .parseSignedClaims(token)
//         .getPayload();
//   }

//   private Key getSigningKey() {
//     return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
//   }
// }
