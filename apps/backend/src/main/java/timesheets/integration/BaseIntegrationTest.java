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
import io.restassured.RestAssured; // java lib for testing REST APIs 
import org.junit.jupiter.api.BeforeEach; //it will run the function with tag before every test - in this case - function that setup the process
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest; // start the Spring Boot for testing 
import org.springframework.boot.test.web.server.LocalServerPort; //for port, when they changes, the RestAssured need to know where to send requests
import org.springframework.test.context.ActiveProfiles; // forces it to use specified profile 
import org.springframework.test.context.DynamicPropertyRegistry; 
import org.springframework.test.context.DynamicPropertySource; 
import org.testcontainers.containers.PostgreSQLContainer; //testcontainers class 
import org.testcontainers.junit.jupiter.Container; //junit will start containers, runs tests and delete container
import org.testcontainers.junit.jupiter.Testcontainers; //testcontainers support in JUnit 

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // assign another random port (NOT 8080 to avoid conflicts)
@TestContainers
@ActiveProfiles("integrator") // use application-test.properties 
// JUnit create a new object for every method so with PER_CLASS, it creates one object and reuse it in the class
@TestInstance(TestInstance.Lifecycle.PER_CLASS) 

public abstract class BaseIntegrationTest {
    @LocalServerPort
    protected int port; //assigns random port 

    // creates a PostgreSQL Docker container
    // docker start this container, creates database and spring connects to it 
    // and when the tests are done, the container stops and deleted 
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withDatabaseName("momently_integration_test").withUsername("integrator").withPassword("test123");

    // how the Spring Boot to connect the database
    // it will connect to the temporary PostgreSQL container
    @DynamicPropertySource
    static void connectToDBS(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::password);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    // verify the port and connect to the right port
    @BeforeEach
    void setup(){
        RestAssured.port = port;
    }
}

/*
JUnit starts -> Testcontainers starts a PostgreSQL Docker container -> PostgreSQL creates the database -> Spring Boot starts the application -> DynamicPropertySource give Spring databse URL, username, password -> Spring connects to PostgreSQL container -> Spring starts on a corect port -> BeforeEach method tells RestAssured to use random port 
*/

