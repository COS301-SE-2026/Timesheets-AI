package timesheets.util;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// this is a util package such that we can reuse in the AuthService and the Mfa services
// it only handles TOTP operations

@Component
public class TotpUtils {
  private final GoogleAuthenticator googleAuthenticator;

  @Value("${app.mfa.issuer:Timesheets AI}")
  private String issuer;

  @Value("${app.mfa.code-length:6}")
  private int codeLength;

  public TotpUtils() {
    this.googleAuthenticator = new GoogleAuthenticator();
  }

  // when a user first enables MFW, a unique key will be generated for them
  public String generateSecret() {
    GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
    return key.getKey();
  }

  // this will create a QR code URL that the authenticator apps can scan, such that the user can set
  // up their authenticator app
  public String generateQrCodeUrl(String secretKey, String email, String issuer) {
    GoogleAuthenticatorKey credentials = new GoogleAuthenticatorKey.Builder(secretKey).build();
    return GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, email, credentials);
  }

  // this func will be called when a user enters their TOTP code to log in or verify their MFA
  public boolean verifyCode(String secretKey, String code) {
    try {
      int codeInt = Integer.parseInt(code);
      return googleAuthenticator.authorize(secretKey, codeInt);
    } catch (NumberFormatException e) {
      return false;
    }
  }
}

// basically the above is the engine behind the MFA system
// code majority from:
// https://medium.com/@shishir-karki/implementing-totp-using-google-auth-in-spring-boot-70cc4381c5e1
