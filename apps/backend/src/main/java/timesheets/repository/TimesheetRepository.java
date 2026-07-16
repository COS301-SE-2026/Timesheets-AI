package timesheets.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.Timesheet;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {
  // find the timesheet for a member in a specific period
  Optional<Timesheet> findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
      UUID workspaceMemberId, LocalDate periodStart, LocalDate periodEnd);

  // find all timesheets for a member
  List<Timesheet> findByWorkspaceMemberId(UUID workspaceMemberId);

  // find the timesheets by status
  List<Timesheet> findByStatus(String status);

  // find timesheets by member and status
  List<Timesheet> findByWorkspaceMemberIdAndStatus(UUID workspaceMemberId, String status);
}
