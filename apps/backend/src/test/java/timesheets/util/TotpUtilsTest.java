package timesheets.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.warrenstrange.googleauth.GoogleAuthenticator;
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

  @Nested
  @DisplayName("generateQrCodeUrl")
  class GenerateQrCodeUrlTests {

    @Test
    @DisplayName("should build an otpauth url containing the secret and issuer")
    void buildsOtpAuthUrl() {
      String secret = totpUtils.generateSecret();

      String url = totpUtils.generateQrCodeUrl(secret, "john.doe@momentum.co.za", "Timesheets");

      assertThat(url).startsWith("https://api.qrserver.com/v1/create-qr-code/");
      assertThat(url).contains("data=otpauth%3A%2F%2Ftotp%2F");
      assertThat(url).contains("secret%3D" + secret);
      assertThat(url).contains("issuer%3DTimesheets");
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCodeTests {

      @Test
      @DisplayName("should accept a currently valid code")
      void acceptsValidCode() {
        String secret = totpUtils.generateSecret();
        int validCode = new GoogleAuthenticator().getTotpPassword(secret);

        assertThat(totpUtils.verifyCode(secret, String.valueOf(validCode))).isTrue();
      }
    }

    @Test
    @DisplayName("should reject a code that is not numeric")
    void rejectsNonNumericCode() {
      String secret = totpUtils.generateSecret();

      assertThat(totpUtils.verifyCode(secret, "abc123")).isFalse();
    }

    @Test
    @DisplayName("should reject an empty code")
    void rejectsEmptyCode() {
      String secret = totpUtils.generateSecret();

      assertThat(totpUtils.verifyCode(secret, "")).isFalse();
    }

    @Test
    @DisplayName("should reject a code generated from a different secret")
    void rejectsCodeFromOtherSecret() {
      String secret = totpUtils.generateSecret();
      String otherSecret = totpUtils.generateSecret();
      int otherCode = new GoogleAuthenticator().getTotpPassword(otherSecret);

      assertThat(totpUtils.verifyCode(secret, String.valueOf(otherCode))).isFalse();
    }
  }
}
