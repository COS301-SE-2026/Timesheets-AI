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
import org.springframework.boot.test.context.SpringBootTest; 
import org.springframework.boot.test.web.server.LocalServerPort; //for port, when they changes, the RestAssured need to know where to send requests
import org.springframework.test.context.ActiveProfiles; // forces it to use specified profile 
import org.springframework.test.context.DynamicPropertyRegistry; 
import org.springframework.test.context.DynamicPropertySource; 
import org.testcontainers.containers.PostgreSQLContainer; //testcontainers class 
import org.testcontainers.junit.jupiter.Container; //junit will start containers, runs tests and delete container
import org.testcontainers.junit.jupiter.Testcontainers; //testcontainers support in JUnit 





