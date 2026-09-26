/*
This handles the response for marking an insight resolved.
mirrors ai-service's ResolveInsightResponse, snake_case mapped by AiServiceClient.

Date: 24/09/2026
*/

package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveInsightResponse {
  private UUID id;
  private boolean resolved;
  private LocalDateTime resolvedAt;
  private UUID resolvedByWorkspaceMemberId;
}
