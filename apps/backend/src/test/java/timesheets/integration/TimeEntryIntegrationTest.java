package timesheets.integration;

import static org.junit.jupiter.api.Assertions.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.repository.TimeEntryRepository;

/** This file: - tests interact with application using testcontainer database */
/*
Extends the BaseIntegrationTest, so we get TestContainers progres, login helpers and restAssured.port wired up
seeded data used from V3__enhance_test_data.sql

Author: Zamokuhle Zwane
Date: 10 August 2026
*/

class TimeEntryIntegrationTest extends BaseIntegrationTest {

  @Autowired private TimeEntryRepository timeEntryRepository;

  // This will use seeded user - to see if the user can be autenticated and receive a JWT token
  private static final UUID VALID_PROJECT_ID =
      UUID.fromString("00000000-0000-0000-0001-000000000200");
  private static final UUID VALID_TASK_ID = UUID.fromString("00000000-0000-0000-0001-000000000220");

  // helper functions

  // this function will build a valid request so each tests has to override what its actually
  // testing
  private TimeEntryRequest validRequest() {
    TimeEntryRequest request = new TimeEntryRequest();
    request.setProjectId(VALID_PROJECT_ID);
    request.setTaskId(VALID_TASK_ID);
    request.setStartTime(LocalDateTime.of(2026, 8, 5, 9, 0));
    request.setEndTime(LocalDateTime.of(2026, 8, 5, 11, 0));
    request.setDurationSeconds(7200);
    request.setEntryType("MANUAL");
    request.setDescription("Integration test entry");
    return request;
  }

  private TimeEntryResponse createEntry(String token, TimeEntryRequest request) {
    return RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/time-entries")
        .then()
        .statusCode(201)
        .extract()
        .as(TimeEntryResponse.class);
  }

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

  // Verify that a user cannot create a time entry without a valid JWT
  // should get ERROR 403
  // Activate Spring Security

  @Test
  void shouldRejectTimeEntryWithoutJWT() {
    TimeEntryRequest request = new TimeEntryRequest();

    // dont login the user because we are trying to test that the system will reject users without a
    // valid token
    // expect status code to be 403 - restrict and forbid users from creating time entry
    request.setProjectId(UUID.fromString("00000000-0000-0000-0001-000000000200"));
    request.setTaskId(UUID.fromString("00000000-0000-0000-0001-000000000220"));
    request.setStartTime(LocalDateTime.of(2026, 8, 4, 9, 0));
    request.setEndTime(LocalDateTime.of(2026, 8, 4, 11, 0));
    request.setDurationSeconds(7200);
    request.setEntryType("MANUAL");
    request.setDescription("Working on another set of UI wireframes");
    RestAssured.given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/time-entries")
        .then()
        .log()
        .body()
        .log()
        .status()
        .statusCode(403);
  }

  // 1. Reject Create A Time Entry Without JWT
  @Test
  void shouldRejectCreateTimeEntryWithoutJwt() {
    //no login() call, hitting the endpoint unauthenticated on purpose this is what actually protects it: timesheets.config.SecurityConfig permits /api/auth/** only, everything else needs a valid Bearer token
    TimeEntryRequest request = validRequest();

    RestAssured.given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/time-entries")
        .then()
        .statusCode(403);
  }

  /*
   2. Reject Invalid Project
  FLAG: TimeEntryService.createTimeEntry() never checks the project exists.
  project_id is a FK with ON DELETE RESTRICT (pls refer to V1__create_initial_schema.sql), so an unknown UUID fails at the DB layer, not the app layer, the GlobalExceptionHandler
  has no handler for DataIntegrityViolationException, so this currently surfaces as raw 500, not a clean 400. Documenting current behavior, not desired behavior
  */
  @Test
  void shouldRejectTimeEntryWithInvalidProject() {
    String token = loginEmployee();

    TimeEntryRequest request = validRequest();
    request.setProjectId(UUID.randomUUID()); //its guaranteed not to exist

    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/time-entries")
        .then()
        .statusCode(500); //FLAG: should be 400 once project existence is validated
  }

  /*
  3. Reject Invalid Task
  FLAG: same gap as invalid project, task_id has no existence check, and no check
  that the task actually belongs to the given project either
  */
  @Test
  void shouldRejectTimeEntryWithInvalidTask() {
    String token = loginEmployee();

    TimeEntryRequest request = validRequest();
    request.setTaskId(UUID.randomUUID());

    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/time-entries")
        .then()
        .statusCode(500); //another FLAG: should be 400 once task existence/ownership is validated
  }

  /* 
    4. Reject Invalid Time Range
    
    FLAG: there is no check anywhere in TimeEntryService that endTime is after startTime This request is backwards and the backend happily saves it as-is.
    Documenting current "wrong" behavior so this test flips red the moment someone adds the validation, that's the signal to come flip this test to expect 400
  */
  @Test
  void currentlyAcceptsInvalidTimeRange() {
    String token = loginEmployee();

    TimeEntryRequest request = validRequest();
    request.setStartTime(LocalDateTime.of(2026, 8, 5, 11, 0));
    request.setEndTime(LocalDateTime.of(2026, 8, 5, 9, 0)); //before it starts

    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/time-entries")
        .then()
        .statusCode(201); //FLAG: should be 400, raising an issue for backend
  }

  //5. Reject Missing Required Fields 
  //this one IS enforced: TimeEntryRequest.projectId has @NotNull, caught by MethodArgumentNotValidException, and GlobalExceptionHandler does handle that one
  @Test
  void shouldRejectTimeEntryWithMissingRequiredFields() {
    String token = loginEmployee();

    TimeEntryRequest request = validRequest();
    request.setProjectId(null); // the only actually-required field

    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/time-entries")
        .then()
        .statusCode(400);
  }

  // 1. Get Current User's Entries
  @Test
  void shouldGetCurrentUsersEntries() {
    String token = loginEmployee();
    TimeEntryResponse created = createEntry(token, validRequest());

    List<TimeEntryResponse> entries =
        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/time-entries/me")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", TimeEntryResponse.class);

    assertTrue(entries.stream().anyMatch(e -> e.getId().equals(created.getId())));
  }

  //2. Get Entry By Id
  @Test
  void shouldGetEntryById() {
    String token = loginEmployee();
    TimeEntryResponse created = createEntry(token, validRequest());

    TimeEntryResponse fetched =
        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/time-entries/" + created.getId())
            .then()
            .statusCode(200)
            .extract()
            .as(TimeEntryResponse.class);

    assertEquals(created.getId(), fetched.getId());
    assertEquals("Integration test entry", fetched.getDescription());
  }

  //3. Update Time Entry
  @Test
  void shouldUpdateTimeEntry() {
    String token = loginEmployee();
    TimeEntryResponse created = createEntry(token, validRequest());

    TimeEntryRequest updateRequest = validRequest();
    updateRequest.setDescription("Updated description");
    updateRequest.setDurationSeconds(3600);

    TimeEntryResponse updated =
        RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .when()
            .put("/api/time-entries/" + created.getId())
            .then()
            .statusCode(200)
            .extract()
            .as(TimeEntryResponse.class);

    assertEquals("Updated description", updated.getDescription());
    assertEquals(3600, updated.getDurationMinutes()); //yes, this field is actually seconds :)
  }

  /*
  4. Reject Invalid Update
    "invalid" here = editing a locked entry, the one update-time business rule that IS implemented (TimeEntryService.updateTimeEntry) It throws a plain RuntimeException("Cannot edit a locked time entry") though, which
    GlobalExceptionHandler doesn't catch it only handles IllegalArgumentException IllegalStateException/MethodArgumentNotValidException), so it falls through to
    Spring's default handler and comes back 500, not a clean 400/409
  */

  @Test
  void shouldRejectUpdateOfLockedTimeEntry() {
    String token = loginEmployee();
    TimeEntryResponse created = createEntry(token, validRequest());

    //manually lock it, the way TimesheetService.submitTimesheet() should in practice
    var entry = timeEntryRepository.findById(created.getId()).orElseThrow();
    entry.setIsLocked(true);
    timeEntryRepository.save(entry);

    TimeEntryRequest updateRequest = validRequest();
    updateRequest.setDescription("should not be allowed to save");

    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(updateRequest)
        .when()
        .put("/api/time-entries/" + created.getId())
        .then()
        .statusCode(500); //FLAGGING: should be 409 Conflict once RuntimeException is handled
  }

  //delete Time Entry
  @Test
  void shouldDeleteTimeEntry() {
    String token = loginEmployee();
    TimeEntryResponse created = createEntry(token, validRequest());

    RestAssured.given()
        .header("Authorization", "Bearer " + token)
        .when()
        .delete("/api/time-entries/" + created.getId())
        .then()
        .statusCode(204);

    //service does a soft delete (isDeleted flag), confirm that rather than expecting the row to be gone entirely
    var deletedEntry = timeEntryRepository.findById(created.getId()).orElseThrow();
    assertTrue(deletedEntry.getIsDeleted());
  }

  //6. Reject Delete Without Authentication
  @Test
  void shouldRejectDeleteWithoutAuthentication() {
    String token = loginEmployee();
    TimeEntryResponse created = createEntry(token, validRequest());

    RestAssured.given()
        //no Authorization header this time
        .when()
        .delete("/api/time-entries/" + created.getId())
        .then()
        .statusCode(403);
  }
  /*
  7. Reject Access To Another User's Entry
  FLAG: this is the big one. TimeEntryService.getTimeEntryById() does not check the ownership at all, it's just a findById(), i dont should happen, any authenticated user can technically GET any
  other user's time entry by id today, Testing what SHOULD happen here (403/404) since TimeEntryAccessDeniedException already exists in the codebase and it implyies that was the intent, but as written this test will currently FAIL with 200. Leaving
  it red on purpose so it shows up in CI until an ownership check is added to the read path, not just update/delete
  */
  @Test
  void shouldRejectAccessToAnotherUsersEntry() {
    String employeeToken = loginEmployee();
    TimeEntryResponse created = createEntry(employeeToken, validRequest());

    String managerToken = loginManager();

    RestAssured.given()
        .header("Authorization", "Bearer " + managerToken)
        .when()
        .get("/api/time-entries/" + created.getId())
        .then()
        .statusCode(Matchers.anyOf(Matchers.is(403), Matchers.is(404)));
    //FLAG: currently returns 200, no ownership check on GET by id
  }
}
