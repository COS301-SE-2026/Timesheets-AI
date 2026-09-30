package timesheets.suggestions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import timesheets.evidence.correlation.EvidenceEngineService;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {
  private final SuggestionService suggestionService;
  private final EvidenceEngineService evidenceEngineService;

  @GetMapping("/workspace-member/{workspaceMemberId}")
  public List<SuggestedWorkSession> getSuggestions(@PathVariable UUID workspaceMemberId) {
    return suggestionService.getSuggestions(workspaceMemberId);
  }

  @GetMapping("/{suggestionId}")
  public SuggestedWorkSession getSuggestion(@PathVariable UUID suggestionId) {
    return suggestionService.getSuggestion(suggestionId);
  }

  @PostMapping("/generate")
  public List<SuggestedWorkSession> generateSuggestions(
      @RequestParam UUID workspaceMemberId,
      @RequestParam LocalDateTime startTime,
      @RequestParam LocalDateTime endTime) {
    return evidenceEngineService.generateSuggestions(workspaceMemberId, startTime, endTime);
  }

  // so it can create time entry and update suggestion status
  @Transactional
  @PostMapping("/{suggestionId}/approve")
  public SuggestedWorkSession approve(@PathVariable UUID suggestionId) {
    return suggestionService.approve(suggestionId);
  }

  @PostMapping("/{suggestionId}/reject")
  public SuggestedWorkSession reject(@PathVariable UUID suggestionId) {
    return suggestionService.reject(suggestionId);
  }

  @PutMapping("/{suggestionId}")
  public SuggestedWorkSession edit(
      @PathVariable UUID suggestionId, @RequestBody EditSuggestionRequest request) {

    return suggestionService.edit(
        suggestionId,
        request.getTitle(),
        request.getProjectId(),
        request.getTaskId(),
        request.getDescription(),
        request.getStartTime(),
        request.getEndTime());
  }
}
