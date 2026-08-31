package timesheets.dto.request;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateProjectRequest {
  private String name;
  private String description;

  @Positive(message = "Budget hours must be positive")
  private BigDecimal budgetHours;

  @Positive(message = "Hourly rate must be positive")
  private BigDecimal hourlyRate;

  @Positive(message = "Budget cost must be positive")
  private BigDecimal budgetCost;

  private LocalDate startDate;
  private LocalDate endDate;
  private List<UUID> managerIds; // incase we need to update project managers
}
