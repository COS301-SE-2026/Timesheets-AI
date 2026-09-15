// concrete observer for the observer design pattern

package timesheets.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.domain.event.LongRunningTimerEvent;
import timesheets.domain.event.TimesheetApprovedEvent;
import timesheets.domain.event.TimesheetRejectedEvent;
import timesheets.domain.event.TimesheetSubmittedEvent;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;

@Component
@RequiredArgsConstructor
public class NotificationEventListenerService {

  private final NotificationService notificationService;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;

  // to notify the admin and manager when someone submits a timesheet
  @EventListener
  public void handleTimesheetSubmitted(TimesheetSubmittedEvent event) {

    // we need to find the workspace that the workspace the member belongs to
    WorkspaceMember submitter =
        workspaceMemberRepository.findById(event.submittedByWorkspaceMemberId()).orElse(null);

    // the notification process should stop when the workpsace member no longer
    // exists
    if (submitter == null) {
      return;
    }

    UUID workspaceId = submitter.getWorkspaceId();

    // when a timesheet is submitted it should notify the manager in the
    // same workspace only managers review submitted timesheets
    List<WorkspaceMember> recipients =
        workspaceMemberRepository.findAllByWorkspaceIdAndRole(
            workspaceId, WorkspaceRole.MANAGER);

    // each recipient should get their own notification so that when the read their
    // notification they do not mark other peoples notifications as read
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

    // here there will only be one recipient since it should be for the workspace
    // member who owns the timesheet
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
  @Async
  @EventListener
  public void handleLongRunningTimer(LongRunningTimerEvent event) {
    // for the in-app notification
    notificationService.createNotification(
        event.workspaceMemberId(),
        "TIMER_LONG_RUNNING",
        "Timer still running",
        "Your timer has been running for more than 8 hours.",
        "TIMER",
        event.timerId());

    // since the event only give the workspace member id, need to get the Workspace
    // Member
    WorkspaceMember workspaceMember =
        workspaceMemberRepository.findById(event.workspaceMemberId()).orElse(null);

    if (workspaceMember == null) {
      return;
    }

    // from the Workspace Member I can then get the user, the actual email and first
    // name
    User user = userRepository.findById(workspaceMember.getUserId()).orElse(null);

    if (user == null) {
      return;
    }

    // if a user does not have an email address then they cannot recievce emails
    if (user.getEmail() == null || user.getEmail().isBlank()) {
      return;
    }

    emailService.sendLongRunningTimerEmail(user.getEmail(), user.getFirstName());
  }

  // also the reason I am making all these backend calls, since in this case we
  // cannot use security context because that is for a signed in user

  // ! NOTE: the reason I have added event listener is so that the method is
  // subsribed to a particular event, the whole observer aspect of it all
}
