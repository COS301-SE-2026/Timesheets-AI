package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.enums.ReactionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleReactionRequest {

  @NotNull(message = "Reaction type is required")
  private ReactionType reactionType;
}
