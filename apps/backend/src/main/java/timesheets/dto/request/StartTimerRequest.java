package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

// remember this is like essentially what backend is expecting from frontend (from the JSON) then it
// create an object here (by deserializing)
// what I mean by desearilizing - converting data from a stored format into a usable object

@Data
public class StartTimerRequest {

  @NotNull(
      message =
          "Project ID is required") // this means that when the response comes the projectID field
  // must not be NULL
  private UUID projectId;

  private UUID taskId;
}
