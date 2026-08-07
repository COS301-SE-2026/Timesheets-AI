package timesheets.integration;

import static org.junit.jupiter.api.Assertions.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.repository.TimeEntryRepository;

/** This file: - tests interact with application using testcontainer database */
class TimeEntryIntegrationTest extends BaseIntegrationTest {

  @Autowired private TimeEntryRepository timeEntryRepository;

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

  // the test below is testing the creating of a valid time entry
  // verify taht authenticated user can create time entry
  @Test
  void shouldCreateTimeEntry() {

    String token = loginEmployee();

    TimeEntryRequest request = new TimeEntryRequest();

    // columns from TimeEntry -
    request.setProjectId(UUID.fromString("00000000-0000-0000-0001-000000000200"));
    request.setTaskId(UUID.fromString("00000000-0000-0000-0001-000000000220"));
    request.setStartTime(LocalDateTime.of(2026, 8, 4, 9, 0));
    request.setEndTime(LocalDateTime.of(2026, 8, 4, 11, 0));
    request.setDurationSeconds(7200);
    request.setEntryType("MANUAL");
    request.setDescription("Working on the UI wireframes");

    TimeEntryResponse response =
        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/time-entries")
            .then()
            .log()
            .body()
            .log()
            .status()
            .statusCode(201)
            .extract()
            .as(TimeEntryResponse.class);

    // Verify repsonse
    assertNotNull(response);
    assertEquals("Working on the UI wireframes", response.getDescription());
    assertEquals("MANUAL", response.getEntryType());
    assertEquals(request.getProjectId(), response.getProjectId());
    assertEquals(request.getTaskId(), response.getTaskId());
  }
}
