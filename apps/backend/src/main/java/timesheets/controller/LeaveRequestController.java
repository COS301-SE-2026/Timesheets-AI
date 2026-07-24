package timesheets.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.dto.request.LeaveRequestRequest;
import timesheets.dto.response.LeaveRequestResponse;
import timesheets.service.LeaveRequestService;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

  private final LeaveRequestService leaveRequestService;

  // this creates a new leave request
  @PostMapping
  public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
      @Valid @RequestBody LeaveRequestRequest.Create request) {

    LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // this updates a leave request
  @PatchMapping("/{id}")
  public ResponseEntity<LeaveRequestResponse> updateLeaveRequest(
      @PathVariable UUID id, @Valid @RequestBody LeaveRequestRequest.Update request) {

    LeaveRequestResponse response = leaveRequestService.updateLeaveRequest(id, request);

    return ResponseEntity.ok(response);
  }

  // this will get the leave requests for a specific user
  @GetMapping("/my-requests")
  public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
    List<LeaveRequestResponse> responses = leaveRequestService.getMyLeaveRequests();

    return ResponseEntity.ok(responses);
  }

  // this will get the leave requests by ID- incase someone needs to see the details about the leave
  // requests
  @GetMapping("/{id}")
  public ResponseEntity<LeaveRequestResponse> getLeaveRequestById(@PathVariable UUID id) {
    LeaveRequestResponse response = leaveRequestService.getLeaveRequestById(id);

    return ResponseEntity.ok(response);
  }

  // this gets the leave requests by status
  @GetMapping
  public ResponseEntity<List<LeaveRequestResponse>> getRequestsByStatus(
      @RequestParam(required = false) String status) {

    if (status != null) {
      List<LeaveRequestResponse> responses = leaveRequestService.getRequestsByStatus(status);

      return ResponseEntity.ok(responses);
    }

    // if the status is not there then all the responses should be returned
    List<LeaveRequestResponse> responses = leaveRequestService.getMyLeaveRequests();
    return ResponseEntity.ok(responses);
  }
}
