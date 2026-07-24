package timesheets.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.LeaveRequest;
import timesheets.dto.request.LeaveRequestRequest;
import timesheets.dto.response.LeaveRequestResponse;
import timesheets.repository.LeaveRequestRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

  private final SecurityUtils securityUtils;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final UserRepository userRepository;

  // this creates a new leave request
  @Transactional
  public LeaveRequestResponse createLeaveRequest(LeaveRequestRequest.Create request) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // to see if the workspace member exists
    workspaceMemberRepository
        .findById(workspaceMemberId)
        .orElseThrow(() -> new RuntimeException("Workspace member not found"));

    // to see if there is an overlap
    boolean hasOverlap =
        leaveRequestRepository.hasOverlappingApprovedLeave(
            workspaceMemberId, request.getEndDate(), request.getStartDate());

    if (hasOverlap) {
      throw new RuntimeException(
          "You already have an approved leave that overlaps with these dates");
    }

    // create a leave request
    LeaveRequest leaveRequest =
        LeaveRequest.builder()
            .workspaceMemberId(workspaceMemberId)
            .leaveType(request.getLeaveType())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .totalDays(request.getTotalDays())
            .reason(request.getReason())
            .attachments(request.getAttachments())
            .status("PENDING")
            .build();

    LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
    return buildLeaveRequestResponse(saved);
  }

  // this updates a leave request
  @Transactional
  public LeaveRequestResponse updateLeaveRequest(
      UUID requestId, LeaveRequestRequest.Update request) {
    LeaveRequest leaveRequest =
        leaveRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));

    // making sure that only pending requests can be updated
    if (!"PENDING".equals(leaveRequest.getStatus())) {
      throw new RuntimeException("Only pending leave requests can be updated");
    }

    // only the requester should be able to update the request
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();
    if (!leaveRequest.getWorkspaceMemberId().equals(currentMemberId)) {
      throw new RuntimeException("You can only update your own leave requests");
    }

    // check if both dates are provided
    if (request.getStartDate() != null && request.getEndDate() != null) {
      if (request.getEndDate().isBefore(request.getStartDate())) {
        throw new RuntimeException("End date cannot be before start date");
      }
    }

    // since this is an update not all the fields will be available
    if (request.getLeaveType() != null) {
      leaveRequest.setLeaveType(request.getLeaveType());
    }

    if (request.getStartDate() != null) {
      leaveRequest.setStartDate(request.getStartDate());
    }

    if (request.getEndDate() != null) {
      leaveRequest.setEndDate(request.getEndDate());
    }

    if (request.getTotalDays() != null) {
      leaveRequest.setTotalDays(request.getTotalDays());
    }

    if (request.getReason() != null) {
      leaveRequest.setReason(request.getReason());
    }

    if (request.getAttachments() != null) {
      leaveRequest.setAttachments(request.getAttachments());
    }

    LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
    return buildLeaveRequestResponse(saved);
  }

  // this will get the users leave requests
  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> getMyLeaveRequests() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    return leaveRequestRepository
        .findByWorkspaceMemberIdOrderByCreatedAtDesc(workspaceMemberId)
        .stream()
        .map(this::buildLeaveRequestResponse)
        .collect(Collectors.toList());
  }

  // this gets the leave requests by the ID
  @Transactional(readOnly = true)
  public LeaveRequestResponse getLeaveRequestById(UUID requestId) {
    LeaveRequest leaveRequest =
        leaveRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));

    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // devs should only view their own leave request
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      if (!leaveRequest.getWorkspaceMemberId().equals(currentMemberId)) {
        throw new RuntimeException("You don't have access to this leave request");
      }
      return buildLeaveRequestResponse(leaveRequest);
    }

    if (securityUtils.isManager() && !securityUtils.isAdmin()) {
      UUID workspaceId = securityUtils.getCurrentWorkspaceId();

      boolean hasAccess =
          leaveRequestRepository.findByWorkspaceId(workspaceId).stream()
              .anyMatch(leaveReq -> leaveReq.getId().equals(requestId));

      if (!hasAccess) {
        throw new RuntimeException("You don't have access to this leave request");
      }
      return buildLeaveRequestResponse(leaveRequest);
    }
    return buildLeaveRequestResponse(leaveRequest);
  }

  /*
  - this gets the leave requests by status
  - admins can view from the entire workspace
  - managers can view from that specific workspace only
  - devs can only view their own
  */
  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> getRequestsByStatus(String status) {

    // developers can only view their own leave requests
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      UUID memberId = securityUtils.getDefaultWorkspaceMemberId();

      return leaveRequestRepository.findByWorkspaceMemberIdAndStatus(memberId, status).stream()
          .map(this::buildLeaveRequestResponse)
          .collect(Collectors.toList());
    }

    // managers can only view all leave requests in their specific workspace
    if (securityUtils.isManager() && !securityUtils.isAdmin()) {
      UUID workspaceId = securityUtils.getCurrentWorkspaceId();

      return leaveRequestRepository.findByWorkspaceIdAndStatus(workspaceId, status).stream()
          .map(this::buildLeaveRequestResponse)
          .collect(Collectors.toList());
    }

    // admins can view leave requests across the entire workspace
    return leaveRequestRepository.findByStatus(status).stream()
        .map(this::buildLeaveRequestResponse)
        .collect(Collectors.toList());
  }

  /*
  - this gets the leave requests within a certain date range
  - admins can view from the entire workspace
  - managers can view from that specific workspace only
  - devs can only view their own
  */
  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> getRequestByDateRange(LocalDate startDate, LocalDate endDate) {

    List<LeaveRequest> requests;

    // devs can only see their own requests
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      UUID memberId = securityUtils.getDefaultWorkspaceMemberId();

      requests =
          leaveRequestRepository.findByWorkspaceMemberIdAndStartDateBetween(
              memberId, startDate, endDate);
    } else if (securityUtils.isManager() && !securityUtils.isAdmin()) {
      UUID workspaceId = securityUtils.getCurrentWorkspaceId();

      requests =
          leaveRequestRepository.findByWorkspaceIdAndStartDateBetween(workspaceId, null, null);
    } else {
      requests = leaveRequestRepository.findByStartDateBetween(startDate, endDate);
    }

    return requests.stream().map(this::buildLeaveRequestResponse).collect(Collectors.toList());
  }

  // ! helper builder
  // this should build the response
  private LeaveRequestResponse buildLeaveRequestResponse(LeaveRequest leaveRequest) {
    String memberName =
        workspaceMemberRepository
            .findById(leaveRequest.getWorkspaceMemberId())
            .map(
                workspaceMember ->
                    userRepository
                        .findById(workspaceMember.getUserId())
                        .map(user -> user.getFirstName() + " " + user.getLastName())
                        .orElse("Unknown User"))
            .orElse("Unknown User");

    // get the name of approver
    String approverName = null;
    if (leaveRequest.getApprovedByWorkspaceMemberId() != null) {
      approverName =
          workspaceMemberRepository
              .findById(leaveRequest.getApprovedByWorkspaceMemberId())
              .map(
                  workspaceMember ->
                      userRepository
                          .findById(workspaceMember.getUserId())
                          .map(user -> user.getFirstName() + " " + user.getLastName())
                          .orElse("Unknown Approver"))
              .orElse("Unknown Approver");
    }

    return LeaveRequestResponse.builder()
        .id(leaveRequest.getId())
        .workspaceMemberId(leaveRequest.getWorkspaceMemberId())
        .memberName(memberName)
        .leaveType(leaveRequest.getLeaveType())
        .startDate(leaveRequest.getStartDate())
        .endDate(leaveRequest.getEndDate())
        .totalDays(leaveRequest.getTotalDays())
        .reason(leaveRequest.getReason())
        .attachments(leaveRequest.getAttachments())
        .status(leaveRequest.getStatus())
        .approvedByName(approverName)
        .approvedAt(leaveRequest.getApprovedAt())
        .rejectionReason(leaveRequest.getRejectionReason())
        .availabilityId(leaveRequest.getAvailabilityId())
        .createdAt(leaveRequest.getCreatedAt())
        .updatedAt(leaveRequest.getUpdatedAt())
        .build();
  }
}
