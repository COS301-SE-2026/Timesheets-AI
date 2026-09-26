/*
Controller for handling insights-related endpoints: personal insights
summary, the AI dashboard proxy (with period support), burnout resolve,
and My Projects for the Developer Insights page

Patch: added period query param to getAiDashboard, added GET /my-projects.
*/

package timesheets.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.AiDashboardResponse;
import timesheets.dto.response.DeveloperProjectResponse;
import timesheets.dto.response.PersonalInsightsResponse;
import timesheets.dto.response.ResolveInsightResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.InsightsService;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

  private final InsightsService insightsService;
  private final SecurityUtils securityUtils;

  public record CurrentMemberResponse(UUID workspaceMemberId) {}

  @GetMapping("/me")
  public ResponseEntity<CurrentMemberResponse> getCurrentMember() {
    return ResponseEntity.ok(
        new CurrentMemberResponse(securityUtils.getDefaultWorkspaceMemberId()));
  }

  @GetMapping("/summary")
  public ResponseEntity<PersonalInsightsResponse> getInsightsSummary(
      @RequestParam LocalDate from, @RequestParam LocalDate to) {

    ProductivityReportRequest request = new ProductivityReportRequest();
    request.setFrom(from);
    request.setTo(to);

    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);
    return ResponseEntity.ok(response);
  }

  // includeResolved defaults false so resolved burnout insights don't keep piling up on screen
  // period defaults 8w, matches ai-service's PeriodOption (4w/8w/12w)
  @GetMapping("/ai")
  public ResponseEntity<AiDashboardResponse> getAiDashboard(
      @RequestParam(required = false, defaultValue = "false") boolean includeResolved,
      @RequestParam(required = false, defaultValue = "8w") String period) {
    return ResponseEntity.ok(insightsService.getAiDashboard(includeResolved, period));
  }

  // marks a burnout (or any) insight as resolved, so it drops off the default list
  @PatchMapping("/{id}/resolve")
  public ResponseEntity<ResolveInsightResponse> resolveInsight(@PathVariable UUID id) {
    return ResponseEntity.ok(insightsService.resolveInsight(id));
  }

  // "My Projects" section on the Developer Insights page
  @GetMapping("/my-projects")
  public ResponseEntity<List<DeveloperProjectResponse>> getMyProjects() {
    return ResponseEntity.ok(insightsService.getMyProjects());
  }
}
