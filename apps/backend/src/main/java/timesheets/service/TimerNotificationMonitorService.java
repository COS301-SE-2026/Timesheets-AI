// this is a Concrete Subject in the Observer pattern

package timesheets.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import timesheets.domain.TimerSession;
import timesheets.domain.event.LongRunningTimerEvent;
import timesheets.repository.TimerSessionRepository;

/*
- checks for timers that have been running for over 8 hours, from time to time
- publishes an event to trigger a notification for the timer owner
- needed since the TimerService only runs when a user performs a timer action
- cannot detect when an active timer passes 8 hours if a user does not do anything
*/

@Component
@RequiredArgsConstructor
public class TimerNotificationMonitorService {

  private final TimerSessionRepository timerSessionRepository;
  private final ApplicationEventPublisher eventPublisher;

  /*
  - this every 10 mins so a user will recieve a notification approximately 10 mins after it exceeds
  - fixedDelay is so that the app instance while the previous one is still running */
  @Scheduled(fixedDelay = 600000)
  public void checkLongRunningTimers() {

    // a timer started before this has been running for approximately 8hrs
    LocalDateTime threshold = LocalDateTime.now().minusHours(8);

    /*
    - I wanted to optimise this because I don't want to make to many requests to the backend
    - The timers recieved are:
        -- currently runnning
        -- have been running for 8+ hours
        -- have not already recieved the notification
    */
    List<TimerSession> longRunningTimers =
        timerSessionRepository.findLongRunningTimersWithoutNotification(threshold);

    // should simply announce that a timer has been running too long
    for (TimerSession timer : longRunningTimers) {
      eventPublisher.publishEvent(
          new LongRunningTimerEvent(timer.getId(), timer.getWorkspaceMemberId()));
    }
  }
}
