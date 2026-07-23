package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// exactly the structure that the frontend is expecting
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTimerResponse {

  private UUID id;
  private SimpleProject project;
  private SimpleTask task;
  private LocalDateTime startedAt;
  private Integer elapsedMinutes;
  private Integer elapsedSeconds;
  private Boolean active;
  private Boolean isPaused;
  private LocalDateTime pausedAt;

  public static ActiveTimerResponse empty() {

    return ActiveTimerResponse.builder().active(false).build();
  }

  // ! using inner classes

  // this is so that I can quickly have all the info about a task and a project in one call about
  // the Timer, such that, I do not have to call the API multiple times yeah??
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleProject {
    private UUID id;
    private String name;
  }

  // ! another inner class
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleTask {
    private UUID id;
    private String title;
  }
}
