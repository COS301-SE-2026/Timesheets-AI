package timesheets.service;

// Handles sending emails for verification and password reset.
// Currently a stub implementation that logs to console.
// Will be replaced with a real email provider (e.g. SendGrid or AWS SES) after Demo 1.
public interface EmailService {

  // sends a verification email with a link containing the token
  public void sendVerificationEmail(String email, String firstName, String token);

  // sends a password reset email with a link containing the token
  public void sendPasswordResetEmail(String email, String firstName, String token);

  void sendGenericEmail(String to, String subject, String htmlContent);
}
