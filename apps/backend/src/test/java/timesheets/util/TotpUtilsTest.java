package timesheets.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TotpUtils Unit Tests")
class TotpUtilsTest {

  private TotpUtils totpUtils;

  @BeforeEach
  void setUp() {
    totpUtils = new TotpUtils();
  }

  @Nested
  @DisplayName("generateSecret")
  class GenerateSecretTests {

    @Test
    @DisplayName("should return a non-empty base32 secret")
    void returnsNonEmptySecret() {
      String secret = totpUtils.generateSecret();

      assertThat(secret).isNotBlank();
      assertThat(secret).matches("[A-Z2-7]+");
    }

    @Test
    @DisplayName("should return a different secret each time")
    void returnsUniqueSecrets() {
      assertThat(totpUtils.generateSecret()).isNotEqualTo(totpUtils.generateSecret());
    }
  }
}
