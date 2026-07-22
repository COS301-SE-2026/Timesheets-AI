package timesheets.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timesheets.domain.TimeEntry;

/*
- this file helps with functions for the database, such that the functions interact with the database
- so what I did here is I just gave the decription of the functions, Spring Boot will be the one that creates the actual queries at runtime
*/

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {

  List<TimeEntry> findByWorkspaceMemberIdOrderByStartTimeDesc(UUID workspaceMemberId);

  List<TimeEntry> findByWorkspaceMemberIdAndStartTimeBetween(
      UUID workspaceMemberId, LocalDateTime start, LocalDateTime end);

  // this is a custom query to get time entries for a user within a date range, we have to join with
  // the workspace_members table to filter by user_id
  // we use nativeQuery = true because we're writing raw SQL instead of JPQL, and we have to use the
  // actual table and column names from the database
  @Query(
      value =
          """
        SELECT te.* FROM time_entries te
        JOIN workspace_members wm ON te.workspace_member_id = wm.id
        WHERE wm.user_id = :userId
        AND te.start_time >= :startTime
        AND te.start_time <= :endTime
        """,
      nativeQuery = true)
  List<TimeEntry> findByUserIdAndDateRange(
      @Param("userId") UUID userId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  List<TimeEntry> findByTimesheetId(UUID timesheetId);

  // lock all entries for a timesheet
  @Modifying
  @Query("UPDATE TimeEntry te SET te.isLocked = true WHERE te.timesheetId = :timesheetId")
  void lockAllByTimesheetId(@Param("timesheetId") UUID timesheetId);

  // unlock all entries for a timesheet
  @Modifying
  @Query("UPDATE TimeEntry te SET te.isLocked = false WHERE te.timesheetId = :timesheetId")
  void unlockAllByTimesheetId(@Param("timesheetId") UUID timesheetId);

  // finds the time entries of a member in a specific project
  List<TimeEntry> findByWorkspaceMemberIdAndProjectId(UUID workspaceMemberId, UUID projectId);

  // finds all the time entries on a particular project
  List<TimeEntry> findByProjectId(UUID projectId);

  // finds all the time entries on a project based on a timeframe
  List<TimeEntry> findByProjectIdAndStartTimeBetween(
      UUID projectId, LocalDateTime start, LocalDateTime end);
}
