package timesheets.service;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Timesheet;
import timesheets.dto.request.TimesheetRequest;
import timesheets.repository.TimesheetRepository;

@Service
@RequiredArgsConstructor
public class TimesheetService {

  private final TimesheetRepository timesheetRepository;

  @Transactional
  public Timesheet createTimesheet(UUID workspaceMemberId, TimesheetRequest request) {
    Timesheet timesheet = new Timesheet();

    timesheet.setWorkspaceMemberId(workspaceMemberId);
    timesheet.setPeriodStart(request.getPeriodStart());
    timesheet.setPeriodEnd(request.getPeriodEnd());
    timesheet.setStatus("DRAFT");
    timesheet.setIsLocked(false);

    return timesheetRepository.save(timesheet);
  }

  // get existing timesheet or create a new one
  @Transactional
  public Timesheet getOrCreateTimesheet(
      UUID workspaceMemberId, LocalDate periodStart, LocalDate periodEnd) {
    return timesheetRepository
        .findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
            workspaceMemberId, periodStart, periodEnd)
        .orElseGet(
            () -> {
              TimesheetRequest request = new TimesheetRequest();
              request.setPeriodStart(periodStart);
              request.setPeriodEnd(periodEnd);
              return createTimesheet(workspaceMemberId, request);
            });
  }

  // get current week's timesheet
  @Transactional
  public Timesheet getOrCreateCurrentTimesheet(UUID workspaceMemberId) {
    LocalDate today = LocalDate.now();

    LocalDate periodStart = today.with(java.time.DayOfWeek.MONDAY);
    LocalDate periodEnd = today.with(java.time.DayOfWeek.SUNDAY);

    return getOrCreateTimesheet(workspaceMemberId, periodStart, periodEnd);
  }
}
