package timesheets.controller;

import jakarta.validation.Valid;
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
}
