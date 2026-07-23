package timesheets.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import timesheets.domain.LeaveRequest;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;


@Repository
public interface LeaveRequestRepository  extends JpaRepository<LeaveRequest, UUID> {

  //finds all the leave requests for a specific workspace member, the newest shows first
  List<LeaveRequest> findByWorkspaceMemberIdOrderByCreatedAtDesc(UUID workspaceMemberId);

  //finds leave requests by status
  List<LeaveRequest> findByStatus(String status);

  //finds all the leave requests for a workspace member by status
  List<LeaveRequest> findByWorkspaceMemberIdAndStatus(UUID workspaceMemberId, String status);

  //finds all the leave requests within a certain date range
  List<LeaveRequest> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

  //finds all the leave requests of a certain member within a certain range
  List<LeaveRequest> findByWorkspaceMemberIdAndStartDateBetween(UUID workspaceMemberId, LocalDate startDate, LocalDate endDate);
}
