/*
this file handles the single endpoint for the manager assistant feature
POST /api/timesheets/{id}/manager-assistant-review

POST not GET on purpose, this call does real work (github db read, live jira and calendar api calls, a gemini call), not a free idempotent fetch, so it
shouldn't look like one. matches how /submit, /approve, /reject are already modeled as POST actions on TimesheetController
nothing runs unless this exact route is hit, which only happens when the manager clicks "AI Review" on an opened timesheet, no auto trigger

Author: Zamokuhle Zwane
Date: 20 September 2026
*/

package timesheets.managerassistant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.domain.Timesheet;
import timesheets.repository.TimesheetRepository;

@RestController
@RequiredArgsConstructor
public class ManagerAssistantController {

  private final TimesheetRepository timesheetRepository;
  private final ManagerAssistantScoringService scoringService;
  private final ManagerAssistantNarrativeClient narrativeClient;

  @PostMapping("/api/timesheets/{id}/manager-assistant-review")
  public ResponseEntity<ManagerAssistantReview> generateReview(@PathVariable UUID id) {
    Timesheet timesheet =
        timesheetRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Timesheet not found: " + id));

    ManagerAssistantReview scored = scoringService.scoreTimesheet(timesheet);
    String narrative = narrativeClient.getNarrative(scored);

    // rebuild with narrative filled in, records are immutable on purpose so a scoring bug can never
    // accidentally leak into the narrative step
    return ResponseEntity.ok(
        new ManagerAssistantReview(
            scored.timesheetId(),
            scored.confidenceScorePercent(),
            scored.verdict(),
            scored.evidenceSources(),
            narrative,
            scored.hasConflict(),
            scored.hasMissingEvidence()));
  }
}
