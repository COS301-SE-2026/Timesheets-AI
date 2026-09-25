package timesheets.suggestions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.SuggestedWorkSessionEntity;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.repository.SuggestionRepository;
import timesheets.service.TimeEntryService;

@Service
@RequiredArgsConstructor
public class SuggestionService {

  private final SuggestionRepository suggestionRepository;
  private final TimeEntryService timeEntryService;

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

    if (suggestion.getStatus() != SuggestionStatus.PENDING
        && suggestion.getStatus() != SuggestionStatus.EDITED) {
      throw new RuntimeException("Only pending or edited suggestions can be approved");
    }

    // the approval creates real time entry

    // Calculate duration in seconds for TimeEntryRequest
    Integer durationSeconds = null;
    if (suggestion.getStartTime() != null && suggestion.getEndTime() != null) {
      durationSeconds =
          (int) Duration.between(suggestion.getStartTime(), suggestion.getEndTime()).toSeconds();
    } else if (suggestion.getDurationMinutes() != null) {
      durationSeconds = suggestion.getDurationMinutes() * 60;
    }

    // Map suggestion parameters into time entry request
    TimeEntryRequest request = new TimeEntryRequest();
    request.setProjectId(suggestion.getProjectId());
    request.setTaskId(suggestion.getTaskId());
    request.setStartTime(suggestion.getStartTime());
    request.setEndTime(suggestion.getEndTime());
    request.setDurationSeconds(suggestion.getDurationMinutes() * 60);
    request.setDescription(suggestion.getTitle());
    request.setEntryType("AI_SUGGESTION");

    timeEntryService.createTimeEntry(request);
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

  @Transactional
  public SuggestedWorkSession edit(
      UUID suggestionId,
      String title,
      UUID projectId,
      UUID taskId,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime) {

    SuggestedWorkSession suggestion = getSuggestion(suggestionId);

    if (suggestion.getStatus() != SuggestionStatus.PENDING) {
      throw new RuntimeException("Only pending suggestions can be edited");
    }

    suggestion.setTitle(title);
    suggestion.setProjectId(projectId);
    suggestion.setTaskId(taskId);
    suggestion.setDescription(description);
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
