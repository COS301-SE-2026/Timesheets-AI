package timesheets.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatedTimeEntryResponse {

  private UUID id;
  private SimpleProject project;
  private SimpleTask task;
  private LocalDate date;
  private LocalTime startTime;
  private LocalTime endTime;
  private Integer durationMinutes;
  private String status;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleProject {
    private UUID id;
    private String name;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleTask {
    private UUID id;
    private String title;
  }
}
