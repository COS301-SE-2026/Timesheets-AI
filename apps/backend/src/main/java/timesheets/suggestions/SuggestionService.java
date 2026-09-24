package timesheets.suggestions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.domain.SuggestedWorkSessionEntity;
import timesheets.repository.SuggestionRepository;

@Service
@RequiredArgsConstructor
public class SuggestionService {

  private final SuggestionRepository suggestionRepository;

  public List<SuggestedWorkSession> getSuggestions(UUID workspaceMemberId) {

    List<SuggestedWorkSessionEntity> entities =
        suggestionRepository.findByWorkspaceMemberIdOrderByStartTimeDesc(workspaceMemberId);

    List<SuggestedWorkSession> suggestions = new ArrayList<SuggestedWorkSession>();

    for (SuggestedWorkSessionEntity entity : entities) {
      suggestions.add(SuggestedWorkSession.fromEntity(entity));
    }

    return suggestions;
  }

  public SuggestedWorkSession getSuggestion(UUID suggestionId) {

    SuggestedWorkSessionEntity entity =
        suggestionRepository
            .findById(suggestionId)
            .orElseThrow(() -> new RuntimeException("No suggestion found: " + suggestionId));

    return SuggestedWorkSession.fromEntity(entity);
  }

  public SuggestedWorkSession save(SuggestedWorkSession suggestion) {

    SuggestedWorkSessionEntity entity = suggestion.toEntity();

    SuggestedWorkSessionEntity saved = suggestionRepository.save(entity);

    return SuggestedWorkSession.fromEntity(saved);
  }

  public SuggestedWorkSession approve(UUID suggestionId) {

    SuggestedWorkSession suggestion = getSuggestion(suggestionId);

    if (suggestion.getStatus() != SuggestionStatus.PENDING) {
      throw new RuntimeException("Only pending suggestions can be approved");
    }

    suggestion.setStatus(SuggestionStatus.APPROVED);

    return save(suggestion);
  }

  public SuggestedWorkSession reject(UUID suggestionId) {

    SuggestedWorkSession suggestion = getSuggestion(suggestionId);

    if (suggestion.getStatus() != SuggestionStatus.PENDING) {
      throw new RuntimeException("Only pending suggestions can be rejected");
    }

    suggestion.setStatus(SuggestionStatus.REJECTED);

    return save(suggestion);
  }

  public SuggestedWorkSession edit(
      UUID suggestionId, String title, LocalDateTime startTime, LocalDateTime endTime) {

    SuggestedWorkSession suggestion = getSuggestion(suggestionId);

    if (suggestion.getStatus() != SuggestionStatus.PENDING) {
      throw new RuntimeException("Only pending suggestions can be edited");
    }

    suggestion.setTitle(title);
    suggestion.setStartTime(startTime);
    suggestion.setEndTime(endTime);

    if (startTime != null && endTime != null) {

      long minutes = Duration.between(startTime, endTime).toMinutes();

      suggestion.setDurationMinutes((int) minutes);
    }

    suggestion.setStatus(SuggestionStatus.EDITED);

    return save(suggestion);
  }
}
