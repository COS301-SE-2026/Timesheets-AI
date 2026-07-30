package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
- honestly these tests are probably going to change as we move towards SMTP */

@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

  // the service being tested, since it does not have dependancies, no need for me to mock it
  private final EmailService emailService = new EmailService();

  // this was suggested to hold all the things that get printed
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  // to restore the System.out after the test since I am using it to see what is printed
  private final PrintStream originalOut = System.out;

  @BeforeEach
  void setUp() {
    /*
    Okay, so I need to capture what gets printed to the console
    Since EmailService is just a stub that uses System.out.println(),
    I'm redirecting System.out to a custom stream so I can check the output.
    */
    System.setOut(new PrintStream(outputStream));
  }

  @AfterEach
  void tearDown() {
    // need to restore it because it might mess with other tests
    System.setOut(originalOut);
  }

  @Test
  @DisplayName("should send verification email with correct details")
  void sendVerificationEmailPrintToConsole() {

    // ARRANGE: need to capture the output, thats what helps with verification
    String testEmail = "john.doe@momentum.co.za";
    String testFirstName = "John";
    String testToken = "abc-123-xyz-verification-token";

    // ACT: calling the actual method
    emailService.sendVerificationEmail(testEmail, testFirstName, testToken);

    // ASSERT
    String consoleOutput = outputStream.toString();

    // to confirms the service is sending to the right person.
    assertThat(consoleOutput).contains(testEmail);

    // very important!! the user needs this token to verify their email.
    assertThat(consoleOutput).contains(testToken);
    assertThat(consoleOutput).contains("Sending verification email to:");
    assertThat(consoleOutput).contains("Verification token: " + testToken);
  }
}
