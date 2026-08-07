package timesheets.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

// this manages the 'blacklist' of JWT tokens,
// since JWTs are stateless and can't be invalidated server-side,
// we need to keep track of blacklisted tokens to effectively log users out.
// we will be using Redis for this because it's fast and supports TTL (time to live) which is
// perfect for expiring blacklisted tokens after their natural expiration time.
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

  private final RedisTemplate<String, String> redisTemplate;
  private final JwtService jwtService;

  private static final String BLACKLIST_PREFIX = "blacklist:";

  public void blacklistToken(String token) {
    // get token expiration to set ttl
    Duration ttl = Duration.ofDays(1); // match jwt expiration
    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted", ttl);
  }

  /*
    While working on the integration tests for Time Entry feature, 
    The login request passed and return a valid JWT token 
    when creating a Time Entry, the request passed through 
    JwtAuthFilter before getting to the controller,  is authenenticating the user, the tokenBlacklistService.isBlacklisted(token) will verify if the token is blacklisted 
    then RedisTemplate.hasKey(...) and it tries to connect to Redis since Redis only exist inside the Docker Compose 
    and at the moment, we dont have 'actual active redis' working 

    why did I get 403?
    in JwTAuthFilter.java
    catch (Exception ignored) {
      // invalid token - just continue unauthenticated
    }

    nothing gets placed into Security Context Holder so it get to Spring Security as an anonymous user and since 
    authentication exists, it will return 403

    so solution: for integration tests, we will mock TokenBlackService so that tokenBlacklistService.isBlacklisted(token)  always return false. 
    This will bypasses Redis, allowing authentication to complete while
    keeping the integration test focused on the Time Entry functionality rather
    than Redis infrastructure.

  */
  public boolean isBlacklisted(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
  }
}
