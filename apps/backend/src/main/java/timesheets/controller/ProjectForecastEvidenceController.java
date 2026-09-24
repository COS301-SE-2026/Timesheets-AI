/*
- this exposes the evidence from gitHub and Jira
*/
package timesheets.controller;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.response.ProjectForecastEvidenceResponse;
import timesheets.service.ProjectForecastEvidenceService;

@RestController
@RequiredArgsConstructor
public class ProjectForecastEvidenceController {

  private final ProjectForecastEvidenceService projectForecastEvidenceService;

  /*
  - this is the endpoint that the ai-service can use to get the external evidence for a project
  - the project id tells the service which project's evidence should be returned
  - startTime and endTime limit the evidence to the period that is relevant to the forecast
  */
  @GetMapping("/api/project-forecast/{projectId}/evidence")
  public ResponseEntity<ProjectForecastEvidenceResponse> getProjectEvidence(
      @PathVariable UUID projectId,
      @RequestParam LocalDateTime startTime,
      @RequestParam LocalDateTime endTime) {

    ProjectForecastEvidenceResponse response =
        projectForecastEvidenceService.getProjectEvidence(projectId, startTime, endTime);
    return ResponseEntity.ok(response);
  }
}
