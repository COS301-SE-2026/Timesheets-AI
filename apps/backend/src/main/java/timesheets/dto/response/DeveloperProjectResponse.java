/*
This handles the response shape for one row in the "My Projects" section
of the Developer Insights page - one per project_members row for the
current developer, active and inactive both included.

Author: Zamokuhle Zwane
Date: 26/09/2026
*/

package timesheets.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperProjectResponse {
  private UUID projectId;
  private String projectName;
  private double hoursLogged;
  private boolean active;
}
