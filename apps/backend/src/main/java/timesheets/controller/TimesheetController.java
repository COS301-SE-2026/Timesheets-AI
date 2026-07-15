package timesheets.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import timesheets.domain.Timesheet;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.RejectRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.dto.response.TimesheetResponse;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.service.TimeEntryService;
import timesheets.service.TimesheetService;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

  private final TimesheetService timesheetService;
  private final TimeEntryService timeEntryService;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;

  private UUID getCurrentWorkspaceMemberId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // gets the Spring Security User
    org.springframework.security.core.userdetails.User springUser =
        (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

    // gets email from Spring Security User
    String email = springUser.getUsername();

    // finds your custom user from database
    User user =
        userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

    // gets workspace member
    return workspaceMemberRepository.findByUserId(user.getId()).stream()
        .findFirst()
        .map(WorkspaceMember::getId)
        .orElseThrow(() -> new RuntimeException("User is not a member of any workspace"));
  }

  // TODO: Replace with actual reviewerId from JWT token after security is implemented
  private UUID getCurrentReviewerId() {
    return UUID.fromString("00000000-0000-0000-0000-000000000002");
  }

  // getting all the timesheets for a logged in user
  @GetMapping("/me")
  public ResponseEntity<List<TimesheetResponse>> getMyTimesheets() {
    UUID memberId = getCurrentWorkspaceMemberId();

    List<Timesheet> timesheets = timesheetService.getTimesheetsByMember(memberId);
    List<TimesheetResponse> responses =
        timesheets.stream().map(TimesheetResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  // getting the timesheet by status
  @GetMapping("/me/status/{status}")
  public ResponseEntity<List<TimesheetResponse>> getMyTimesheetsByStatus(
      @PathVariable String status) {
    UUID memberId = getCurrentWorkspaceMemberId();

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
    UUID reviewerId = getCurrentReviewerId();

    Timesheet timesheet = timesheetService.approveTimesheet(id, reviewerId);

    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }

  // when a timesheet gets rejected
  @PostMapping("/{id}/reject")
  public ResponseEntity<TimesheetResponse> rejectTimesheet(
      @PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
    UUID reviewerId = getCurrentReviewerId();
    Timesheet timesheet = timesheetService.rejectTimesheet(id, reviewerId, request.getReason());

    return ResponseEntity.ok(TimesheetResponse.from(timesheet));
  }
}
