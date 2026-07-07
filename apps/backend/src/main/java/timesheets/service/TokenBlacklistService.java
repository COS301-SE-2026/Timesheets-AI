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

  public boolean isBlacklisted(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
  }
}
