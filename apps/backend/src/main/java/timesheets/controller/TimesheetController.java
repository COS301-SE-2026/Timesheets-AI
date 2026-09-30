package timesheets.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.domain.Timesheet;
import timesheets.dto.request.RejectRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.dto.response.TimesheetResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.TimeEntryService;
import timesheets.service.TimesheetService;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

  private final TimesheetService timesheetService;
  private final TimeEntryService timeEntryService;
  private final SecurityUtils securityUtils;

  @Operation(
      summary = "Get my timesheets",
      description = "Gets all timesheets for the current user.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timesheets retrieved successfully")
  })
  @GetMapping("/me")
  public ResponseEntity<List<TimesheetResponse>> getMyTimesheets() {

    UUID memberId = securityUtils.getDefaultWorkspaceMemberId();

    List<Timesheet> timesheets = timesheetService.getTimesheetsByMember(memberId);

    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(
      summary = "Get my timesheets by status",
      description = "Gets the current user's timesheets filtered by status.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timesheets retrieved successfully")
  })
  @GetMapping("/me/status/{status}")
  public ResponseEntity<List<TimesheetResponse>> getMyTimesheetsByStatus(
      @PathVariable String status) {
    UUID memberId = securityUtils.getDefaultWorkspaceMemberId();
    List<Timesheet> timesheets = timesheetService.getTimesheetsByMemberAndStatus(memberId, status);

    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "Get timesheet", description = "Gets a specific timesheet by its ID.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timesheet retrieved successfully"),
    @ApiResponse(responseCode = "404", description = "Timesheet not found")
  })
  @GetMapping("/{id}")
  public ResponseEntity<TimesheetResponse> getTimesheetById(@PathVariable UUID id) {
    Timesheet timesheet = timesheetService.getTimesheetById(id);
    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  @Operation(
      summary = "Get timesheet entries",
      description = "Gets all time entries for a timesheet.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Time entries retrieved successfully"),
    @ApiResponse(responseCode = "404", description = "Timesheet not found")
  })
  @GetMapping("/{id}/entries")
  public ResponseEntity<List<TimeEntryResponse>> getTimesheetEntries(@PathVariable UUID id) {
    List<TimeEntryResponse> entries = timeEntryService.getEntriesByTimesheet(id);

    return ResponseEntity.ok(entries);
  }

  @Operation(summary = "Submit timesheet", description = "Submits a timesheet for review.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timesheet submitted successfully"),
    @ApiResponse(responseCode = "404", description = "Timesheet not found"),
    @ApiResponse(responseCode = "409", description = "Timesheet has already been submitted")
  })
  @PostMapping("/{id}/submit")
  public ResponseEntity<TimesheetResponse> submitTimesheet(@PathVariable UUID id) {
    Timesheet timesheet = timesheetService.submitTimesheet(id);

    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  @Operation(summary = "Approve timesheet", description = "Approves a submitted timesheet.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timesheet approved successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to approve timesheets"),
    @ApiResponse(responseCode = "404", description = "Timesheet not found"),
    @ApiResponse(responseCode = "409", description = "Timesheet cannot be approved")
  })
  @PostMapping("/{id}/approve")
  public ResponseEntity<TimesheetResponse> approveTimesheet(@PathVariable UUID id) {

    UUID reviewerId = securityUtils.getDefaultWorkspaceMemberId();
    Timesheet timesheet = timesheetService.approveTimesheet(id, reviewerId);

    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  @Operation(summary = "Reject timesheet", description = "Rejects a submitted timesheet.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timesheet rejected successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to reject timesheets"),
    @ApiResponse(responseCode = "404", description = "Timesheet not found"),
    @ApiResponse(responseCode = "409", description = "Timesheet cannot be rejected")
  })
  @PostMapping("/{id}/reject")
  public ResponseEntity<TimesheetResponse> rejectTimesheet(
      @PathVariable UUID id, @Valid @RequestBody RejectRequest request) {

    UUID reviewerId = securityUtils.getDefaultWorkspaceMemberId();
    Timesheet timesheet = timesheetService.rejectTimesheet(id, reviewerId, request.getReason());

    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  @Operation(
      summary = "Get workspace timesheets",
      description = "Gets timesheets from the current workspace.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Workspace timesheets retrieved successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to view workspace timesheets")
  })
  @GetMapping("/workspace")
  public ResponseEntity<List<TimesheetResponse>> getWorkspaceTimesheets() {
    List<Timesheet> timesheets = timesheetService.getWorkspaceTimesheets();

    // converts the timesheets into a response and puts them in a list, so certain things are not
    // exposed
    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  /*
  - managers and admins
  - gets all the submitted timesheets in the users workspace
  - the first-time submitted and the resubmitted both can be her
  - think viewing other peoples timesheets
   */
  @Operation(
      summary = "Get pending timesheets",
      description = "Gets submitted timesheets waiting for review in the current workspace.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pending timesheets retrieved successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to view pending timesheets")
  })
  @GetMapping("/workspace/pending")
  public ResponseEntity<List<TimesheetResponse>> getPendingWorkspaceTimesheets() {

    List<Timesheet> timesheets = timesheetService.getPendingWorkspaceTimesheets();

    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  // viewing the timesheets by the status in that workspace
  // think viewing other peoples timesheets by the status
  @Operation(
      summary = "Get workspace timesheets by status",
      description = "Gets workspace timesheets filtered by status.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Timesheets retrieved successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to view workspace timesheets")
  })
  @GetMapping("/workspace/status/{status}")
  public ResponseEntity<List<TimesheetResponse>> getWorkspaceTimesheetsByStatus(
      @PathVariable String status) {

    List<Timesheet> timesheets = timesheetService.getWorkspaceTimesheetsByStatus(status);

    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }
}
