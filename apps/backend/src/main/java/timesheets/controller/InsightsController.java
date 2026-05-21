package timesheets.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import timesheets.domain.User;
import timesheets.dto.response.InsightsSummaryResponse;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.service.InsightsService;

import java.time.LocalDate;

//controller for handling insights-related endpoints,
//currently includes an endpoint for generating a summary of insights based on time entries for a given user
//and date range, with various metrics and breakdowns

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {
    
    private final InsightsService insightsService;
    
    @GetMapping("/summary") //endpoint for generating insights summary, accepts from and to dates as query parameters
    public ResponseEntity<InsightsSummaryResponse> getInsightsSummary(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @AuthenticationPrincipal User currentUser) {
        
        ProductivityReportRequest request = new ProductivityReportRequest();
        request.setFrom(from); 
        request.setTo(to);
        
        InsightsSummaryResponse response = insightsService.getInsightsSummary(request, currentUser);//call service to generate insights summary based on time entries for the authenticated user in the specified date range
        return ResponseEntity.ok(response);
    }
}