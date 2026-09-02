package exception;

import lombok.Getter;

/*
- this is an exception class that shows us the various ways to handles the auth failures
- I used this such that I don't have to have multiple auth classes instead having a central class is better for maintainability
*/

@Getter
public class AuthException extends RuntimeException {

  // this is the particular error code, used for the message format and HHTP mapping
  private final ErrorCode errorCode;

  // used with formatting error message
  private final Object[] args;

  public enum ErrorCode {
    INVALID_CREDENTIALS("Invalid credentials"),
    ACCOUNT_LOCKED("Account locked. Please try again in %d minutes"),

    SSO_USER("This account uses Google SSO. Please sign in with Google."),
    EMAIL_EXISTS("Email already exists. Please login or use a different email."),
    EMAIL_DOMAIN("Email domain not accepted. Please use a valid company email."),
    EMAIL_NOT_VERIFIED("This email not verified."),

    TOKEN_NOT_FOUND("Invalid verification token"),
    TOKEN_EXPIRED("Verification token has expired"),
    TOKEN_USED("Token already used"),

    USER_NOT_FOUND("User not found"),
    ACCOUNT_NOT_CONFIGURED("Account not properly configured. Contact support.");

    private final String message;

    // this will be the message template
    ErrorCode(String message) {
      this.message = message;
    }

    // will format the message with the specific arguments
    public String getFormattedMessage(Object... args) {
      return String.format(message, args);
    }
  }

  // this will create the auth exception with the specific error code + arguments
  public AuthException(ErrorCode errorCode, Object... args) {
    super(errorCode.getFormattedMessage(args));
    this.errorCode = errorCode;
    this.args = args;
  }
}
