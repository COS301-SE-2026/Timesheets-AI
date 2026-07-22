package timesheets.controller;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.ProductivityReportResponse;
import timesheets.service.ReportsService;

// controller for handling report-related endpoints,
// currently includes an endpoint for generating a productivity report based on time entries for a
// given user and date range
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

  private final ReportsService reportsService;

  // endpoint for generating a productivity report,
  // accepts from and to dates as query parameters
  // and returns a summary of time entries for the authenticated user in that date range

  @GetMapping(
      "/productivity") // endpoint for generating productivity report, accepts from and to dates as
  // query parameters
  public ResponseEntity<ProductivityReportResponse> getProductivityReport(
      @RequestParam LocalDate from, @RequestParam LocalDate to) {
    // build request object
    ProductivityReportRequest request = new ProductivityReportRequest();
    request.setFrom(from);
    request.setTo(to);

    ProductivityReportResponse report = reportsService.generateProductivityReport(request);
    return ResponseEntity.ok(report);
  }
}
