package timesheets.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timesheets.domain.LeaveRequest;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

  // finds all the leave requests for a specific workspace member, the newest shows first
  List<LeaveRequest> findByWorkspaceMemberIdOrderByCreatedAtDesc(UUID workspaceMemberId);

  // finds leave requests by status
  List<LeaveRequest> findByStatus(String status);

  // finds all the leave requests for a workspace member by status
  List<LeaveRequest> findByWorkspaceMemberIdAndStatus(UUID workspaceMemberId, String status);

  // finds all the leave requests within a certain date range
  List<LeaveRequest> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

  // finds all the leave requests of a certain member within a certain range
  List<LeaveRequest> findByWorkspaceMemberIdAndStartDateBetween(
      UUID workspaceMemberId, LocalDate startDate, LocalDate endDate);

  // this will find all the leave requests on all workspaces, last created first
  List<LeaveRequest> findAllByOrderByCreatedAtDesc();

  // this will find all the leave requests on all workspaces, earliest created first
  List<LeaveRequest> findAllByOrderByCreatedAtAsc();

  // finds all the leave requests in a specific workspace, gets newest first
  // managers should be able to view all the leave requests in a specific workspace
  @Query(
      "SELECT leaveRequest FROM LeaveRequest leaveRequest "
          + "JOIN WorkspaceMember workspaceMember ON leaveRequest.workspaceMemberId = workspaceMember.id "
          + "WHERE workspaceMember.workspaceId = :workspaceId "
          + "ORDER BY leaveRequest.createdAt DESC")
  List<LeaveRequest> findByWorkspaceId(@Param("workspaceId") UUID workspaceId);

  // finds all the leave requests in a specific workspace, by status, gets newest first
  // managers should be able to view all the leave requests in a specific workspace
  @Query(
      "SELECT leaveRequest FROM LeaveRequest leaveRequest "
          + "JOIN WorkspaceMember workspaceMember ON leaveRequest.workspaceMemberId = workspaceMember.id "
          + "WHERE workspaceMember.workspaceId = :workspaceId "
          + "AND leaveRequest.status = :status "
          + "ORDER BY leaveRequest.createdAt DESC")
  List<LeaveRequest> findByWorkspaceIdAndStatus(
      @Param("workspaceId") UUID workspaceId, @Param("status") String status);

  // finds all the leave requests in a specific workspace, between certain dates, gets newest first
  // managers should be able to view all the leave requests in a specific workspace
  @Query(
      "SELECT leaveRequest FROM LeaveRequest leaveRequest "
          + "JOIN WorkspaceMember workspaceMember ON leaveRequest.workspaceMemberId = workspaceMember.id "
          + "WHERE workspaceMember.workspaceId = :workspaceId "
          + "AND leaveRequest.startDate BETWEEN :startDate AND :endDate "
          + "ORDER BY leaveRequest.startDate ASC")
  List<LeaveRequest> findByWorkspaceIdAndStartDateBetween(
      @Param("workspaceId") UUID workspaceId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  // finds if the workspace member has any leave requests that are approved and overlap with range
  // the actual query was alot better than the function name
  @Query(
      "SELECT CASE WHEN COUNT(leaveRequest) > 0 THEN true ELSE false END "
          + "FROM LeaveRequest leaveRequest "
          + "WHERE leaveRequest.workspaceMemberId = :memberId "
          + "AND leaveRequest.status = 'APPROVED' "
          + "AND leaveRequest.startDate <= :requestedEndDate "
          + "AND leaveRequest.endDate >= :requestedStartDate")
  boolean hasOverlappingApprovedLeave(
      @Param("memberId") UUID workspaceMemberId,
      @Param("requestedStartDate") LocalDate requestedStartDate,
      @Param("requestedEndDate") LocalDate requestedEndDate);
}
