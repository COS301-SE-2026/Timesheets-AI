package timesheets.service;

import java.util.UUID;
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
