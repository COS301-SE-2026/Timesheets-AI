package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TokenBlacklistService Unit Tests")
class TokenBlacklistServiceTest {

  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private JwtService jwtService;

  @InjectMocks private TokenBlacklistService tokenBlacklistService;

  private final String token = "some.jwt.token";

  @Nested
  @DisplayName("blacklistToken")
  class BlacklistTokenTests {

    @Test
    @DisplayName("should store the token with a one day ttl")
    void storesTokenWithTtl() {
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);

      tokenBlacklistService.blacklistToken(token);

      verify(valueOperations).set("blacklist:" + token, "blacklisted", Duration.ofDays(1));
    }
  }

  @Nested
  @DisplayName("isBlacklisted")
  class IsBlacklistedTests {

    @Test
    @DisplayName("should return true when the key exists")
    void trueWhenKeyExists() {
      when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(true);

      assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }

    @Test
    @DisplayName("should return false when the key does not exist")
    void falseWhenKeyMissing() {
      when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(false);

      assertThat(tokenBlacklistService.isBlacklisted(token)).isFalse();
    }

    @Test
    @DisplayName("should return false when redis returns null")
    void falseWhenRedisReturnsNull() {
      when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(null);

      assertThat(tokenBlacklistService.isBlacklisted(token)).isFalse();
    }

    @Test
    @DisplayName("should return false when redis is unavailable")
    void falseWhenRedisFails() {
      when(redisTemplate.hasKey("blacklist:" + token))
          .thenThrow(new QueryTimeoutException("redis down"));

      assertThat(tokenBlacklistService.isBlacklisted(token)).isFalse();
    }
  }
}
