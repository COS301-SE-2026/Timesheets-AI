package timesheets.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** This file: - tests interact with application using testcontainer database */
class TimeEntryIntegrationTest extends BaseIntegrationTest {

  // This will use seeded user - to see if the user can be autenticated and receive a JWT token

  @Test
  void shouldLoginEmployee() {

    // call parent for this function - which will returns a token
    String token = loginEmployee();

    // this should ensure a valid JWT token returns
    assertNotNull(token);

    // it should not be blank
    assertFalse(token.isBlank());

    // JWT tokens should have 3 BASE64 sections
    assertEquals(3, token.split("\\.").length);
  }
}
