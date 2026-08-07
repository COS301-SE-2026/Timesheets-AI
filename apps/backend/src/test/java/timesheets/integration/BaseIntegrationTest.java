/*
   Every integration test files will inherit from this class
   To avoid duplicate or repetitive code setup

   It will handle the following:
   - Start PostgreSQL using Testcontainers
   - Start the Spring Boot application
   -  Run Flyway miggrations
   - Configure to use MockMvc (confirmed in pom.xml) NO
   - Now, using RestAssured is better for integration test: https://stackoverflow.com/questions/52051570/whats-the-difference-between-mockmvc-restassured-and-testresttemplate?rq=1
   - Include Helper functions such as login methods, it call POST /api/auth/login and extracts the JWT and return it
*/

package timesheets.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import timesheets.dto.request.AuthRequest;
import timesheets.dto.response.AuthResponse;

@SpringBootTest(
    webEnvironment =
        SpringBootTest.WebEnvironment
            .RANDOM_PORT) // assign another random port (NOT 8080 to avoid conflicts)
@Testcontainers
@ActiveProfiles("test") // use application-test.properties
// JUnit create a new object for every method so with PER_CLASS, it creates one object and reuse it
// in the class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

  @LocalServerPort protected int port; // assigns random port

  // creates a PostgreSQL Docker container
  // docker start this container, creates database and spring connects to it
  // and when the tests are done, the container stops and deleted
  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("momently_integration_test")
          .withUsername("integrator")
          .withPassword("test123");


  // how the Spring Boot to connect the database
  // it will connect to the temporary PostgreSQL container
  @DynamicPropertySource
  static void connectToDBS(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
  }

  // verify the port and connect to the right port
  @BeforeEach
  void setup() {
    RestAssured.port = port;
  }

  protected static final String password = "momentlyPass300$";
  protected static final String employee_email = "enzokuhle.khumalo@momentum.co.za";
  protected static final String manager_email = "amahle.dlamini@momentum.co.za";
  protected static final String admin_email = "karabo.mathebula@momentum.co.za";

  // login function

  protected String login(String email) {
    AuthRequest request = new AuthRequest();
    request.setEmail(email);
    request.setPassword(password);

    AuthResponse response =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .as(AuthResponse.class);

    return response.getToken();
  }

  // helper methods

  protected String loginEmployee() {
    return login(employee_email);
  }

  protected String loginManager() {
    return login(manager_email);
  }

  protected String loginAdmin() {
    return login(admin_email);
  }
}

/*
JUnit starts -> Testcontainers starts a PostgreSQL Docker container -> PostgreSQL creates the database -> Spring Boot starts the application -> DynamicPropertySource give Spring databse URL, username, password -> Spring connects to PostgreSQL container -> Spring starts on a corect port -> BeforeEach method tells RestAssured to use random port
*/
