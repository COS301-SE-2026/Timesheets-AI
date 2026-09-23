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
}
