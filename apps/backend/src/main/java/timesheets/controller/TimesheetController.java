package timesheets.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

  // getting all the timesheets for a logged in user
  @GetMapping("/me")
  public ResponseEntity<List<TimesheetResponse>> getMyTimesheets() {

    UUID memberId = securityUtils.getDefaultWorkspaceMemberId();

    List<Timesheet> timesheets = timesheetService.getTimesheetsByMember(memberId);
    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  // getting the timesheet by status
  @GetMapping("/me/status/{status}")
  public ResponseEntity<List<TimesheetResponse>> getMyTimesheetsByStatus(
      @PathVariable String status) {
    UUID memberId = securityUtils.getDefaultWorkspaceMemberId();
    List<Timesheet> timesheets = timesheetService.getTimesheetsByMemberAndStatus(memberId, status);
    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  // getting a single timesheet
  @GetMapping("/{id}")
  public ResponseEntity<TimesheetResponse> getTimesheetById(@PathVariable UUID id) {
    Timesheet timesheet = timesheetService.getTimesheetById(id);
    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  // getting all the entries of a timesheet
  @GetMapping("/{id}/entries")
  public ResponseEntity<List<TimeEntryResponse>> getTimesheetEntries(@PathVariable UUID id) {
    List<TimeEntryResponse> entries = timeEntryService.getEntriesByTimesheet(id);
    return ResponseEntity.ok(entries);
  }

  // when a timesheet is approved
  @PostMapping("/{id}/submit")
  public ResponseEntity<TimesheetResponse> submitTimesheet(@PathVariable UUID id) {
    Timesheet timesheet = timesheetService.submitTimesheet(id);
    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  // when a timesheet gets approved
  @PostMapping("/{id}/approve")
  public ResponseEntity<TimesheetResponse> approveTimesheet(@PathVariable UUID id) {

    UUID reviewerId = securityUtils.getCurrentUserId();
    Timesheet timesheet = timesheetService.approveTimesheet(id, reviewerId);

    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  // when a timesheet gets rejected
  @PostMapping("/{id}/reject")
  public ResponseEntity<TimesheetResponse> rejectTimesheet(
      @PathVariable UUID id, @Valid @RequestBody RejectRequest request) {

    UUID reviewerId = securityUtils.getCurrentUserId();
    Timesheet timesheet = timesheetService.rejectTimesheet(id, reviewerId, request.getReason());

    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }
}
