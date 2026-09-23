package timesheets.suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SuggestionService {
  private final List<SuggestedWorkSession> suggestions = new ArrayList<SuggestedWorkSession>();

  public List<SuggestedWorkSession> getSuggestions(UUID workspaceMemberId) {
    List<SuggestedWorkSession> result = new ArrayList<SuggestedWorkSession>();

    for (SuggestionWorkSession suggestion : suggestions) {
      if (suggestion.getWorkspaceMemberId().equals(workspaceMemberId)) {
        result.add(suggestion);
      }
    }

    return result;
  }

  public SuggestedWorkSession getSuggestion(UUID suggestionId) {
    for (SuggestedWorkSession suggestion : suggestions) {
      if (suggestion.getid().equals(suggestionId)) {
        return suggestion;
      }
    }

    throw new RuntimeException("No suggestion found:" + suggestion);
  }

  public SuggestedWorkSession save(SuggestedWorkSession suggestion){
    suggestions.add(suggestion);
    return suggestion;
  }

   public SuggestedWorkSession approve(UUID suggestionId){
    SuggestedWorkSession suggestion = getSuggestion(suggestionId);
    suggestion.setStatus(SuggestionStatus.APPROVED);

    return suggestion;
   }

   public SuggestedWorkSession reject(UUID suggestionId){
    SuggestedWorkSession suggestion = getSuggestion(suggestionId);
    suggestion.setStatus(SuggestionStatus.REJECTED);

    return suggestion;
   }

   public SuggestedWorkSession edit(UUID suggestionId, String title, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime){
    SuggestedWorkSession suggestion = getSuggestion(suggestionId);

    suggestion.setTitle(title);
    suggestion.setStartTime(startTime);
    suggestion.setEndTime(endTime);

    if (startTime != null && endTime != null){
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();

        suggestion.setDurationMinutes((int) minutes);
    }

    suggestion.setStatus(SuggestionStatus.EDITED);

    return suggestion;
   }
}
