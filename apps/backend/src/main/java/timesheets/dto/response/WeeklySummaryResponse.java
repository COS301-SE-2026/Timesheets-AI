/*
Mirrors ai-service's WeeklySummaryResponse, snake_case mapped the same way everything else off AiServiceClient is.

Author: Zamokuhle Zwane
Date: 27/09/2026
*/

package timesheets.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySummaryResponse {
  private UUID subjectId;
  private String subjectType;
  private LocalDate weekStart;
  private String narrative;
  private UUID insightId;
}
