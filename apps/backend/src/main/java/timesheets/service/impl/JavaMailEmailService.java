package timesheets.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import timesheets.service.EmailService;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailService implements EmailService {

  private final JavaMailSender mailSender;

  @Value("${app.email.from}")
  private String fromEmail;

  @Value("${app.email.from-name}")
  private String fromName;

  @Value("${app.base-url}")
  private String baseUrl;

  @Override
  public void sendVerificationEmail(String email, String firstName, String token) {

    String subject = "Verify your email - Timesheets AI";
    String htmlContent = buildVerificationEmailHtml(firstName, token);

    sendEmail(email, subject, htmlContent);
  }

  @Override
  public void sendPasswordResetEmail(String email, String firstName, String token) {

    String subject = "Reset your password - Timesheets AI";
    String htmlContent = buildPasswordResetEmailHtml(firstName, token);

    sendEmail(email, subject, htmlContent);
  }

  @Override
  public void sendGenericEmail(String to, String subject, String htmlContent) {
    sendEmail(to, subject, htmlContent);
  }

  private void sendEmail(String to, String subject, String htmlContent) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail, fromName);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);
      mailSender.send(message);
      log.info("Email successfully to: {}", to);
    } 
    catch (Exception e) {
      log.error("Failed to send email to: {}", to, e);
    }
  }

  private String buildVerificationEmailHtml(String firstName, String token) {
    String verificationLink = baseUrl + "/verify-email?token=" + token;
    return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #2563eb; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 30px; background: #f9fafb; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; padding: 12px 24px; background: #2563eb; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Timesheets AI</h1>
                    </div>
                    <div class="content">
                        <h2>Welcome, %s!</h2>
                        <p>Thank you for signing up for Timesheets AI. Please verify your email address.</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verify Email</a>
                        </p>
                        <p>This link will expire in 24 hours.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Timesheets AI</p>
                    </div>
                </div>
            </body>
            </html>
            """
        .formatted(firstName, verificationLink);
  }

  private String buildPasswordResetEmailHtml(String firstName, String token) {
    String resetLink = baseUrl + "/reset-password?token=" + token;
    return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #2563eb; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 30px; background: #f9fafb; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; padding: 12px 24px; background: #2563eb; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Timesheets AI</h1>
                    </div>
                    <div class="content">
                        <h2>Hello, %s!</h2>
                        <p>We received a request to reset your password.</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Reset Password</a>
                        </p>
                        <p>This link will expire in 1 hour.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Timesheets AI</p>
                    </div>
                </div>
            </body>
            </html>
            """
        .formatted(firstName, resetLink);
  }
}
