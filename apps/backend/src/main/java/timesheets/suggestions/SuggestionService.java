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

  @Transactional
  public SuggestedWorkSession approve(UUID suggestionId) {

    SuggestedWorkSession suggestion = getSuggestion(suggestionId);

    if (suggestion.getStatus() != SuggestionStatus.PENDING
        && suggestion.getStatus() != SuggestionStatus.EDITED) {
      throw new RuntimeException("Only pending or edited suggestions can be approved");
    }

    Integer durationSeconds = null;

    if (suggestion.getDurationMinutes() != null && suggestion.getDurationMinutes() > 0) {
      durationSeconds = suggestion.getDurationMinutes() * 60;
    } else if (suggestion.getStartTime() != null
        && suggestion.getEndTime() != null
        && suggestion.getEndTime().isAfter(suggestion.getStartTime())) {
      durationSeconds =
          (int) Duration.between(suggestion.getStartTime(), suggestion.getEndTime()).toSeconds();
    }

    if (durationSeconds == null || durationSeconds <= 0) {
      throw new RuntimeException("Duration is required before this suggestion can be approved");
    }

    TimeEntryRequest request = new TimeEntryRequest();
    request.setProjectId(suggestion.getProjectId());
    request.setTaskId(suggestion.getTaskId());
    request.setStartTime(suggestion.getStartTime());
    request.setEndTime(suggestion.getEndTime());
    request.setDurationSeconds(durationSeconds);
    request.setDescription(suggestion.getDescription());
    request.setEntryType("AI_SUGGESTION");

    timeEntryService.createTimeEntry(request);

    suggestion.setStatus(SuggestionStatus.APPROVED);

    return save(suggestion);
  }

  @Transactional
  public SuggestedWorkSession reject(UUID suggestionId) {

    SuggestedWorkSession suggestion = getSuggestion(suggestionId);

    if (suggestion.getStatus() != SuggestionStatus.PENDING
        && suggestion.getStatus() != SuggestionStatus.EDITED) {
      throw new RuntimeException("Only pending or edited suggestions can be rejected");
    }

    suggestion.setStatus(SuggestionStatus.REJECTED);

    return save(suggestion);
  }

  // update: include projectId and task so it can be editted
  // projectId msut show the avaiiable projects and maybe dropdown??
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

    if (suggestion.getStatus() != SuggestionStatus.PENDING
        && suggestion.getStatus() != SuggestionStatus.EDITED) {
      throw new RuntimeException("Only pending or edited suggestions can be edited");
    }

    suggestion.setTitle(title);
    suggestion.setProjectId(projectId);
    suggestion.setTaskId(taskId);
    suggestion.setDescription(description);
    suggestion.setStartTime(startTime);
    suggestion.setEndTime(endTime);

    if (startTime != null && endTime != null && endTime.isAfter(startTime)) {

      long minutes = Duration.between(startTime, endTime).toMinutes();

      if (minutes > 0) {
        suggestion.setDurationMinutes((int) minutes);
      } else {
        suggestion.setDurationMinutes(null);
      }

    } else {
      suggestion.setDurationMinutes(null);
    }

    suggestion.setStatus(SuggestionStatus.EDITED);

    return save(suggestion);
  }
}
