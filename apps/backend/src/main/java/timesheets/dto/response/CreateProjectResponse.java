package timesheets.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class CreateProjectResponse {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @Positive(message = "Budget hours must be positive")
    private BigDecimal budgetHours;

    private BigDecimal hourlyRate;

    @NotNull(message = "Manager IDs are required")
    private List<UUID> managerIds; //the workspace member Ids of managers to assign
}
