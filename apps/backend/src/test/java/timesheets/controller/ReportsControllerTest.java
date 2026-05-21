package timesheets.controller;
//unit tests for the reports controller, which provides endpoints for generating various reports and insights based on time entry data
//these tests focus on the productivity report endpoint, which returns a summary of the developer's time entries over a specified date range, including total hours logged, tasks worked on, and breakdowns by project and task
//the tests verify that the endpoint returns the expected data structure and values, and also check for proper authentication and error handling when required parameters are missing or the user is not authenticated
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import timesheets.config.JwtAuthFilter;
import timesheets.config.SecurityConfig;
import timesheets.domain.User;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.ProductivityReportResponse;
import timesheets.repository.UserRepository;
import timesheets.service.JwtService;
import timesheets.service.ReportsService;
import timesheets.service.TokenBlacklistService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

//mocking the dependencies of the ReportsController to isolate the controller logic and test its behavior in response to various inputs and authentication states
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//these tests ensure that the ReportsController correctly handles requests to generate productivity reports, including validating input parameters, enforcing authentication, and returning the expected response structure and data based on the mocked service layer
@WebMvcTest(ReportsController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ReportsControllerTest {

    @Autowired MockMvc mockMvc; //this allows us to perform HTTP requests against the controller and verify the responses without starting a full server
    @Autowired ObjectMapper objectMapper;

    @MockBean ReportsService reportsService;
    @MockBean JwtService jwtService;
    @MockBean TokenBlacklistService tokenBlacklistService;
    @MockBean UserRepository userRepository;

    private final User mockUser = User.builder() // this mock user represents an authenticated developer making requests to the reports endpoint, and is used in the tests to simulate authenticated access
        .email("bob@momentum.co.za")
        .emailVerified(true)
        .build();

    @Test
    void getProductivityReport_returns200WithValidResponse() throws Exception { 
        //must return 200 OK with a valid response body when the request is properly authenticated and contains valid parameters,
        //and the service layer returns a mocked response
        ProductivityReportResponse mockResponse = ProductivityReportResponse.builder()
            .generatedAt(LocalDateTime.now())
            .period(ProductivityReportResponse.Period.builder()
                .from(LocalDate.of(2026, 5, 1))
                .to(LocalDate.of(2026, 5, 20))
                .build())
            .summary(ProductivityReportResponse.Summary.builder() 
            //mocked summary data that the service layer would return based on the time entries for the specified date range,
            //which the controller should include in the response body
                .totalHoursLogged(13.0)
                .totalEntriesLogged(2)
                .tasksWorkedOn(2)
                .projectsWorkedOn(1)
                .build())
            .byTask(List.of())
            .byWeek(List.of())
            .build();

        when(reportsService.generateProductivityReport(any(ProductivityReportRequest.class), any(User.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/reports/productivity")//perform a GET request to the productivity report endpoint with the required query parameters and authenticated user context
                .param("from", "2026-05-01")
                .param("to", "2026-05-20")
                .with(SecurityMockMvcRequestPostProcessors.user(mockUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.totalHoursLogged").value(13.0))
            .andExpect(jsonPath("$.summary.totalEntriesLogged").value(2))
            .andExpect(jsonPath("$.period.from").value("2026-05-01"));
    }

    @Test
    void getProductivityReport_returns403WhenNotAuthenticated() throws Exception {
        //the endpoint should return 403 Forbidden when the request is made without an authenticated user context, even if the required parameters are present
        mockMvc.perform(get("/api/reports/productivity")
                .param("from", "2026-05-01")
                .param("to", "2026-05-20"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bob@momentum.co.za")
    void getProductivityReport_returns400WhenMissingParams() throws Exception {
        //the endpoint should return 400 Bad Request when required query parameters are missing, even if the user is authenticated
        mockMvc.perform(get("/api/reports/productivity"))
            .andExpect(status().isBadRequest());
    }
}
