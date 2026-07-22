package timesheets.controller;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.PersonalInsightsResponse;
import timesheets.service.InsightsService;

// controller for handling insights-related endpoints,
// currently includes an endpoint for generating a summary of insights based on time entries for a
// given user
// and date range, with various metrics and breakdowns

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

  private final InsightsService insightsService;

  @GetMapping(
      "/summary") // endpoint for generating insights summary, accepts from and to dates as query
  // parameters
  public ResponseEntity<PersonalInsightsResponse> getInsightsSummary(
      @RequestParam LocalDate from, @RequestParam LocalDate to) {

    ProductivityReportRequest request = new ProductivityReportRequest();
    request.setFrom(from);
    request.setTo(to);

    // call service to generate insights summary based on time entries for the
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);
    // authenticated user in the specified date range
    return ResponseEntity.ok(response);
  }
}
