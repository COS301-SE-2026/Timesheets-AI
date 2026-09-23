package timesheets.suggestions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {
    private final SuggestionService suggestionService;

    @GetMapping("/workspace-member/{workspaceMemberId}")
    public List<SuggestedWorkSession> getSuggestions(@PathVariable UUID workspaceMemberId){
        return suggestionService.getSuggestions(workspaceMemberId);
    }

    @GetMapping("/{suggestionId}")
    public SuggestedWorkSession getSuggestion(@PathVariable UUID getSuggestionId){
        return suggestionService.getSuggestion(suggestionId);
    }

    
}