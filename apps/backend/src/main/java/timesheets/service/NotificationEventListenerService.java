package timesheets.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import timesheets.domain.WorkspaceMember;
import timesheets.domain.event.LongRunningTimerEvent;
import timesheets.domain.event.TimesheetApprovedEvent;
import timesheets.domain.event.TimesheetRejectedEvent;
import timesheets.domain.event.TimesheetSubmittedEvent;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.WorkspaceMemberRepository;

@Component
@RequiredArgsConstructor
public class NotificationEventListenerService {

  private final NotificationService notificationService;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  // to notify the admin and manager when someone submits a timesheet
  @EventListener
  public void handleTimesheetSubmitted(TimesheetSubmittedEvent event) {

    WorkspaceMember submitter =
        workspaceMemberRepository.findById(event.submittedByWorkspaceMemberId()).orElse(null);

    if (submitter == null) {
      return;
    }

    UUID workspaceId = submitter.getWorkspaceId();

    List<WorkspaceMember> recipients = new ArrayList<>();

    recipients.addAll(
        workspaceMemberRepository.findAllByWorkspaceIdAndRole(workspaceId, WorkspaceRole.ADMIN));

    recipients.addAll(
        workspaceMemberRepository.findAllByWorkspaceIdAndRole(workspaceId, WorkspaceRole.MANAGER));

    for (WorkspaceMember recipient : recipients) {

      notificationService.createNotification(
          recipient.getId(),
          "TIMESHEET_SUBMITTED",
          "Timesheet submitted",
          "A timesheet has been submitted and is waiting for review.",
          "TIMESHEET",
          event.timesheetId());
    }
  }

  // to tell the owner that their timesheet has been approved
  @EventListener
  public void handleTimesheetApproved(TimesheetApprovedEvent event) {

    notificationService.createNotification(
        event.ownerWorkspaceMemberId(),
        "TIMESHEET_APPROVED",
        "Timesheet approved",
        "Your timesheet has been approved.",
        "TIMESHEET",
        event.timesheetId());
  }

  // to tell the owner that their timesheet has been rejected
  @EventListener
  public void handleTimesheetRejected(TimesheetRejectedEvent event) {

    notificationService.createNotification(
        event.ownerWorkspaceMemberId(),
        "TIMESHEET_REJECTED",
        "Timesheet rejected",
        "Your timesheet has been rejected. Please review it.",
        "TIMESHEET",
        event.timesheetId());
  }

  // to tell someone that their timer has been running for longer than 8 hours
  @EventListener
  public void handleLongRunningTimer(LongRunningTimerEvent event) {

    notificationService.createNotification(
        event.workspaceMemberId(),
        "TIMER_LONG_RUNNING",
        "Timer still running",
        "Your timer has been running for more than 8 hours.",
        "TIMER",
        event.timerId());
  }
}
