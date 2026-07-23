package timesheets.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
- all the leave requests dto's
- inner classes will handle specific logic
- it's easier to keep them all organised in one file and preventing cluttering the dto folder */
public class LeaveRequestRequest {

  // creating a new leave request
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Create {
    @NotNull(message = "Leave type is required")
    @Pattern(
        regexp = "ANNUAL|SICK|MATERNITY|PATERNITY|FAMILY_RESPONSIBILITY|OTHER",
        message =
            "Leave type must be ANNUAL, SICK, MATERNITY, PATERNITY, FAMILY_RESPONSIBILITY or OTHER")
    private String leaveType;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in future")
    private LocalDate endDate;

    @Positive(message = "Total days must be positive")
    private Double totalDays;

    private String reason;

    // this is going to be a JSON string of attachements
    private String attachments;
  }

  /*
  - request to update a leave request
  - only pending requests should be updated
  - rejected or approved requests should not be updatable */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Update {
    @Pattern(
        regexp = "ANNUAL|SICK|MATERNITY|PATERNITY|FAMILY_RESPONSIBILITY|OTHER",
        message =
            "Leave type must be ANNUAL, SICK, MATERNITY, PATERNITY, FAMILY_RESPONSIBILITY or OTHER")
    private String leaveType;

    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @Future(message = "End date must be in future")
    private LocalDate endDate;

    @Positive(message = "Total days must be positive")
    private Double totalDays;

    private String reason;

    // this is going to be a JSON string of attachements
    private String attachments;
  }

  // request for approving a leave request
  @Data
  @Builder
  public static class Approve {
    // no approval fields
  }

  // request for rejecting a leave request
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Reject {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
  }

  // request for cancelling a leave request
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Cancel {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
  }
}
