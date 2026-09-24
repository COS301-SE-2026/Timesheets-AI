package timesheets.evidence.correlation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.GitHubEvidenceCollector;
import timesheets.evidence.JiraEvidenceCollector;
import timesheets.suggestions.SuggestedWorkSession;
import timesheets.suggestions.SuggestionGenerationService;
import timesheets.suggestions.SuggestionService;

@Service
@RequiredArgsConstructor
public class EvidenceEngineService {
  private final GitHubEvidenceCollector githubEvidenceCollector;
  private final JiraEvidenceCollector jiraEvidenceCollector;
  private final EvidenceCorrelationService evidenceCorrelationService;
  private final SuggestionGenerationService suggestionGenerationService;
  private final SuggestionService suggestionService;

  public List<SuggestedWorkSession> generateSuggestions(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>();

    evidenceEvents.addAll(githubEvidenceCollector.collect(workspaceMemberId, startTime, endTime));
    evidenceEvents.addAll(jiraEvidenceCollector.collect(workspaceMemberId, startTime, endTime));

    List<EvidenceGroup> groups =
        evidenceCorrelationService.correlate(workspaceMemberId, evidenceEvents);
    List<SuggestedWorkSession> suggestions =
        suggestionGenerationService.generateSuggestions(groups);
    List<SuggestedWorkSession> savedSuggestions = new ArrayList<SuggestedWorkSession>();

    for (SuggestedWorkSession suggestion : suggestions) {
      savedSuggestions.add(suggestionService.save(suggestion));
    }

    return savedSuggestions;
  }
}
