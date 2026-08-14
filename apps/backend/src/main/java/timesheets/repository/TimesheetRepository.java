package timesheets.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  // ! manager queries
  /*
    MANAGER
    - this will get all the timesheets in a specific workspace
    - they will only see submitted, approved, rejected
    - they cannot see the drafts because they are not ready for view

    - the join is to get specifically the timesheets based on the workspace member id that user is in
    - will probably need this for reporting and analytics??
  */
  @Query(
      "SELECT timesheet FROM Timesheet timesheet "
          + "JOIN WorkspaceMember workspaceMember ON timesheet.workspaceMemberId = workspaceMember.id "
          + "WHERE workspaceMember.workspaceId = :workspaceId "
          + "AND timesheet.status != 'DRAFT' "
          + "ORDER BY timesheet.createdAt DESC")
  List<Timesheet> findByWorkspaceIdExcludingDraft(@Param("workspaceId") UUID workspaceId);

  /*
  MANAGER
  - gets all the timsheets in the workspace that are not drafts
  - managers will not be able to see their own timesheets in the workspace
   */

  @Query(
      "SELECT timesheet FROM Timesheet timesheet "
          + "JOIN WorkspaceMember workspaceMember ON timesheet.workspaceMemberId = workspaceMember.id "
          + "WHERE workspaceMember.workspaceId = :workspaceId "
          + "AND timesheet.status != 'DRAFT' "
          + "AND timesheet.workspaceMemberId != :currentMemberId "
          + "ORDER BY timesheet.createdAt DESC")
  List<Timesheet> findByWorkspaceIdExcludingDraftAndMember(
      @Param("workspaceId") UUID workspaceId, @Param("currentMemberId") UUID currentMemberId);

  /*
  MANAGER
  - this will get all the timesheets in a specific workspace by that status
  - managers should not be able to see their own timesheets in the workspace
  - will help with the filtering
   */
  @Query(
      "SELECT timesheet FROM Timesheet timesheet "
          + "JOIN WorkspaceMember workspaceMember ON timesheet.workspaceMemberId = workspaceMember.id "
          + "WHERE workspaceMember.workspaceId = :workspaceId "
          + "AND timesheet.status = :status "
          + "AND timesheet.workspaceMemberId != :currentMemberId "
          + "ORDER BY timesheet.createdAt DESC")
  List<Timesheet> findByWorkspaceIdAndStatusExcludingMember(
      @Param("workspaceId") UUID workspaceId,
      @Param("status") String status,
      @Param("currentMemberId") UUID currentMemberId);

  // MANGER: this will just give all the submitted timesheets, those submitted and resubmitted
  // managers should not be able to see their own timesheets
  default List<Timesheet> findPendingByWorkspaceIdExcludingMember(
      UUID workspaceId, UUID currentMemberId) {
    return findByWorkspaceIdAndStatusExcludingMember(workspaceId, "SUBMITTED", currentMemberId);
  }

  // ! admin queries
  /*
    ADMIN
    - this will get all the timesheets in a specific workspace by that status
    - will help with the filtering
  */
  @Query(
      "SELECT timesheet FROM Timesheet timesheet "
          + "JOIN WorkspaceMember workspaceMember ON timesheet.workspaceMemberId = workspaceMember.id "
          + "WHERE workspaceMember.workspaceId = :workspaceId "
          + "AND timesheet.status = :status "
          + "ORDER BY timesheet.createdAt DESC")
  List<Timesheet> findByWorkspaceIdAndStatus(
      @Param("workspaceId") UUID workspaceId, @Param("status") String status);

  // ADMIN: this will just give all the submitted timesheets, those submitted and resubmitted
  default List<Timesheet> findPendingByWorkspaceId(UUID workspaceId) {
    return findByWorkspaceIdAndStatus(workspaceId, "SUBMITTED");
  }
}
