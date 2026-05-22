package timesheets.controller;
//unit tests for the InsightsController, which is responsible for handling requests related to insights and productivity reports in the timesheets application.
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
import timesheets.dto.response.InsightsSummaryResponse;
import timesheets.repository.UserRepository;
import timesheets.service.InsightsService;
import timesheets.service.JwtService;
import timesheets.service.TokenBlacklistService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any; //allows us to specify that any instance is acceptable for a given parameter in a mocked method call, which is useful when we don't care about the specific value being passed and just want to verify that the method is called with any instance of the expected type
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//summary of the responses for the InsightsController's endpoints:
//200 good request
//400 bad request
//403 forbidden
//500 internal server error
//these tests check the behavior of the InsightsController's endpoints under different conditions, such as valid requests, missing parameters, and authentication issues.

@WebMvcTest(InsightsController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class InsightsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean InsightsService insightsService;
    @MockBean JwtService jwtService;
    @MockBean TokenBlacklistService tokenBlacklistService;
    @MockBean UserRepository userRepository;

    private final User mockUser = User.builder()
    //creates a mock user object with the specified properties
    //which can be used in tests to simulate an authenticated user when testing the InsightsController's endpoints
        .email("bob@momentum.co.za")
        .emailVerified(true)
        .build();

    @Test
    void getInsightsSummary_returns200WithValidResponse() throws Exception {
        //this test checks that when a valid request is made to the /api/insights/summary endpoint with the required parameters and an authenticated user,
        //the controller returns a 200 OK status and the expected JSON response containing the insights summarydata 
        InsightsSummaryResponse mockResponse = InsightsSummaryResponse.builder()
            .totalHoursLogged(13.0)
            .averageHoursPerDay(0.65)
            .totalDaysLogged(2)
            .hoursPerProject(List.of())
            .hoursPerTask(List.of())
            .dailyTrend(List.of())
            .build();

        when(insightsService.getInsightsSummary(any(ProductivityReportRequest.class), any(User.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/insights/summary")
                .param("from", "2026-05-01")
                .param("to", "2026-05-20")
                .with(SecurityMockMvcRequestPostProcessors.user(mockUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalHoursLogged").value(13.0))
            .andExpect(jsonPath("$.totalDaysLogged").value(2))
            .andExpect(jsonPath("$.averageHoursPerDay").value(0.65));
    }

    @Test
    void getInsightsSummary_returns403WhenNotAuthenticated() throws Exception {
        //this test checks that when a request is made to the /api/insights/summary endpoint without authentication,
        //the controller returns a 403 Forbidden status, indicating that the user is not authorized to access the insights summary data without proper authentication.
        mockMvc.perform(get("/api/insights/summary")
                .param("from", "2026-05-01")
                .param("to", "2026-05-20"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bob@momentum.co.za")
    void getInsightsSummary_returns400WhenMissingParams() throws Exception {
        //this test checks that when a request is made to the /api/insights/summary endpoint with an authenticated user but missing required parameters,
        //the controller returns a 400 Bad Request status, indicating that the request is invalid due to missing parameters.
        mockMvc.perform(get("/api/insights/summary"))
            .andExpect(status().isBadRequest());
    }
}
