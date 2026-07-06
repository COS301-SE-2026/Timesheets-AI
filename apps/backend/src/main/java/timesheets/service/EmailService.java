package timesheets.service;

import org.springframework.stereotype.Service;

// Handles sending emails for verification and password reset.
// Currently a stub implementation that logs to console.
// Will be replaced with a real email provider (e.g. SendGrid or AWS SES) after Demo 1.
@Service
public class EmailService {

  // sends a verification email with a link containing the token
  public void sendVerificationEmail(String email, String firstName, String token) {
    System.out.println("Sending verification email to: " + email);
    System.out.println("Verification token: " + token);
  }

  // sends a password reset email with a link containing the token
  public void sendPasswordResetEmail(String email, String firstName, String token) {
    System.out.println("Sending password reset email to: " + email);
    System.out.println("Reset token: " + token);
  }
}
