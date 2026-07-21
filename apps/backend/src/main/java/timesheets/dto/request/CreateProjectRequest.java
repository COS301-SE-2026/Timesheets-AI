package timesheets.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateProjectRequest {

  @NotBlank(message = "Project name is required")
  private String name;

  private String description;

  @Positive(message = "Budget hours must be positive")
  private BigDecimal budgetHours;

  @Positive(message = "Hourly rate must be positive")
  private BigDecimal hourlyRate;

  @Positive(message = "Budget cost must be positive")
  private BigDecimal budgetCost;

  @PastOrPresent(message = "Start date cannot be in the future")
  private LocalDate startDate;

  @Future(message = "End date must be in the future")
  private LocalDate endDate;

  @NotNull(message = "Manager IDs are required")
  private List<UUID> managerIds; // the workspace member Ids of managers to assign
}
