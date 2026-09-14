package timesheets.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import timesheets.service.EmailService;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailService implements EmailService {

  // springs mail sender
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

  // this is what will send a reminder when a users timer has been running for more than 8 hours
  @Override
  public void sendLongRunningTimerEmail(String email, String firstName) {

    String subject = "Your timer is still running - Timesheets AI";
    String htmlContent = buildLongRunningTimerEmailHtml(firstName);

    sendEmail(email, subject, htmlContent);
  }

  // this is for creating and sending the HTML email
  private void sendEmail(String to, String subject, String htmlContent) {

    try {
      MimeMessage message = mailSender.createMimeMessage();

      // true because this email contains inline images
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // for the basic email info
      helper.setFrom(fromEmail, fromName);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);

      // to embed the Momently logo into the email
      helper.addInline(
          "momentlyName", new ClassPathResource("email/momently-name.png"), "image/png");

      // want to make sure that the hour glass is embedded for template that use it
      // it was previously showing as an attatchement
      if (includeHourglass) {
        helper.addInline("hourglass", new ClassPathResource("email/hourglass.png"), "image/png");
      }

      mailSender.send(message);

      // these logs have been a big help for me to see what is happening
      log.info("Email successfully sent to: {}", to);

    } catch (Exception e) {
      log.error("Failed to send email to: {}", to, e);
    }
  }

  private String buildVerificationEmailHtml(String firstName, String token) {

    String verificationLink = baseUrl + "/verify-email?token=" + token;

    return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>

                <body style="margin: 0; padding: 0; background-color: #E6F1FB; font-family: Arial, Helvetica, sans-serif; color: #444444;">

                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #E6F1FB; padding: 40px 16px;">
                        <tr>
                            <td align="center">

                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 560px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; border: 1px solid #BFD4F4;">

                                    <!-- Momently Name -->
                                    <tr>
                                        <td align="center" style="padding: 32px 32px 14px 32px;">
                                            <img src="cid:momentlyName" alt="Momently" style="display: block; max-width: 180px; height: auto;">
                                        </td>
                                    </tr>

                                    <!-- Hourglass -->
                                    <tr>
                                        <td align="center" style="padding: 10px 32px 16px 32px;">
                                            <img src="cid:hourglass" alt="Momently hourglass" width="110" style="display: block; width: 110px; max-width: 110px; height: auto;">
                                        </td>
                                    </tr>

                                    <!-- Main Content -->
                                    <tr>
                                        <td align="center" style="padding: 0 42px 36px 42px;">

                                            <h1 style="margin: 8px 0 18px 0; color: #0F4C91; font-size: 28px; line-height: 36px; font-weight: 700;">
                                                Verify your email
                                            </h1>

                                            <p style="margin: 0 0 12px 0; font-size: 15px; line-height: 24px; color: #444444;">
                                                Hi %s,
                                            </p>

                                            <p style="margin: 0 auto 26px auto; max-width: 420px; font-size: 15px; line-height: 24px; color: #444444;">
                                                Thanks for joining Momently.
                                                Verify your email address to start making every moment count.
                                            </p>


                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 0 auto;">
                                                <tr>
                                                    <td align="center" bgcolor="#0F4C91" style="border-radius: 8px;">
                                                        <a href="%s" style="display: inline-block; padding: 14px 32px; color: #FFFFFF; font-size: 15px; font-weight: 700; text-decoration: none; background-color: #0F4C91; border-radius: 8px;">
                                                            VERIFY EMAIL
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>


                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-top: 30px; background-color: #FEF0E6; border-radius: 8px;">
                                                <tr>
                                                    <td align="center" style="padding: 12px 18px; color: #8A4B20; font-size: 13px; line-height: 20px;">
                                                        This verification link expires in <strong>24 hours</strong>.
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin: 26px 0 0 0; color: #6B7280; font-size: 13px; line-height: 20px;">
                                                If you did not create a Momently account, you can safely ignore this email.
                                            </p>

                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="background-color: #F8FBFF; border-top: 1px solid #E6F1FB; padding: 22px 30px;">
                                            <p style="margin: 0; color: #6B7280; font-size: 11px; line-height: 18px;">
                                                &copy; 2026 Momently
                                                <br>
                                                Time tracking that drives productivity, not paperwork.
                                            </p>
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

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
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>

                <body style="margin: 0; padding: 0; background-color: #E6F1FB; font-family: Arial, Helvetica, sans-serif; color: #444444;">

                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #E6F1FB; padding: 40px 16px;">
                        <tr>
                            <td align="center">

                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 560px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; border: 1px solid #BFD4F4;">

                                    <!-- Momently Name -->
                                    <tr>
                                        <td align="center" style="padding: 32px 32px 14px 32px;">
                                            <img src="cid:momentlyName" alt="Momently" style="display: block; max-width: 180px; height: auto;">
                                        </td>
                                    </tr>

                                    <!-- Hourglass -->
                                    <tr>
                                        <td align="center" style="padding: 10px 32px 16px 32px;">
                                            <img src="cid:hourglass" alt="Momently hourglass" width="110" style="display: block; width: 110px; max-width: 110px; height: auto;">
                                        </td>
                                    </tr>

                                    <!-- Main Content -->
                                    <tr>
                                        <td align="center" style="padding: 0 42px 36px 42px;">

                                            <h1 style="margin: 8px 0 18px 0; color: #0F4C91; font-size: 28px; line-height: 36px; font-weight: 700;">
                                                Reset your password
                                            </h1>

                                            <p style="margin: 0 0 12px 0; font-size: 15px; line-height: 24px; color: #444444;">
                                                Hi %s,
                                            </p>

                                            <p style="margin: 0 auto 26px auto; max-width: 420px; font-size: 15px; line-height: 24px; color: #444444;">
                                                We received a request to reset your Momently password.
                                                Use the button below to choose a new password.
                                            </p>

                                            <!-- RESET BUTTON -->
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 0 auto;">
                                                <tr>
                                                    <td align="center" bgcolor="#0F4C91" style="border-radius: 8px;">
                                                        <a href="%s" style="display: inline-block; padding: 14px 32px; color: #FFFFFF; font-size: 15px; font-weight: 700; text-decoration: none; background-color: #0F4C91; border-radius: 8px;">
                                                            RESET PASSWORD
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>


                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-top: 30px; background-color: #FEF0E6; border-radius: 8px;">
                                                <tr>
                                                    <td align="center" style="padding: 12px 18px; color: #8A4B20; font-size: 13px; line-height: 20px;">
                                                        This password reset link expires in <strong>1 hour</strong>.
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin: 26px 0 0 0; color: #6B7280; font-size: 13px; line-height: 20px;">
                                                If you did not request a password reset, you can safely ignore this email.
                                            </p>

                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="background-color: #F8FBFF; border-top: 1px solid #E6F1FB; padding: 22px 30px;">
                                            <p style="margin: 0; color: #6B7280; font-size: 11px; line-height: 18px;">
                                                &copy; 2026 Momently
                                                <br>
                                                Time tracking that drives productivity, not paperwork.
                                            </p>
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

                </body>
                </html>
                """
        .formatted(firstName, resetLink);
  }

  private String buildLongRunningTimerEmailHtml(String firstName) {

    String loginLink = baseUrl + "/login";

    return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>

                <body style="margin: 0; padding: 0; background-color: #E6F1FB; font-family: Arial, Helvetica, sans-serif; color: #444444;">

                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #E6F1FB; padding: 40px 16px;">
                        <tr>
                            <td align="center">

                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 560px; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; border: 1px solid #BFD4F4;">

                                    <!-- Momently Name -->
                                    <tr>
                                        <td align="center" style="padding: 36px 32px 24px 32px;">
                                            <img src="cid:momentlyName" alt="Momently" style="display: block; max-width: 180px; height: auto;">
                                        </td>
                                    </tr>

                                    <!-- Main Content -->
                                    <tr>
                                        <td align="center" style="padding: 0 42px 36px 42px;">

                                            <h1 style="margin: 8px 0 18px 0; color: #0F4C91; font-size: 28px; line-height: 36px; font-weight: 700;">
                                                Your timer is still running
                                            </h1>

                                            <p style="margin: 0 0 12px 0; font-size: 15px; line-height: 24px; color: #444444;">
                                                Hi %s,
                                            </p>

                                            <p style="margin: 0 auto 26px auto; max-width: 420px; font-size: 15px; line-height: 24px; color: #444444;">
                                                Your Momently timer has been running for more than 8 hours.
                                                If you are no longer working, please remember to stop your timer.
                                            </p>


                                            <!-- OPEN MOMENTLY BUTTON -->
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 0 auto;">
                                                <tr>
                                                    <td align="center" bgcolor="#0F4C91" style="border-radius: 8px;">
                                                        <a href="%s" style="display: inline-block; padding: 14px 32px; color: #FFFFFF; font-size: 15px; font-weight: 700; text-decoration: none; background-color: #0F4C91; border-radius: 8px;">
                                                            OPEN MOMENTLY
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin: 26px 0 0 0; color: #6B7280; font-size: 13px; line-height: 20px;">
                                                This is an automatic reminder to help keep your time records accurate.
                                            </p>

                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="background-color: #F8FBFF; border-top: 1px solid #E6F1FB; padding: 22px 30px;">
                                            <p style="margin: 0; color: #6B7280; font-size: 11px; line-height: 18px;">
                                                &copy; 2026 Momently
                                                <br>
                                                Time tracking that drives productivity, not paperwork.
                                            </p>
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

                </body>
                </html>
                """
        .formatted(firstName, loginLink);
  }
}
