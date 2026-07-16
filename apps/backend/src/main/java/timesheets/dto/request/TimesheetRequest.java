package timesheets.dto.request;

import java.time.LocalDate;
import lombok.Data;

@Data
public class TimesheetRequest {
  private LocalDate periodStart;
  private LocalDate periodEnd;
}
