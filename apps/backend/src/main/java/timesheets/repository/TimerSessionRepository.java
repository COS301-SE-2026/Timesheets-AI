package timesheets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timesheets.domain.TimerSession;

// okay so I am going to use the method of allowing Spring to parse through the method names to get
// an SQL query
// I like it, it's faster and somewhat easier, but I will use SQL here and there yk??

// ! I want to prevent users from starting multiple timers across different workspaces.

@Repository
public interface TimerSessionRepository extends JpaRepository<TimerSession, UUID> {

  // should find an active timer of a member by their workspace_id
  Optional<TimerSession> findByWorkspaceMemberIdAndIsRunningTrue(UUID workspaceMemberId);

  // ! the reason I am using optional is because this might not exist and optional forces a case
  // where this does not exist to be handled.
  // ! if I used null I might get null exceptions

  // sees if the user has an active timer
  boolean existsByWorkspaceMemberIdAndIsRunningTrue(UUID workspaceMemberId);

  // this will check the active timer of a user, it can't be more than 1
  @Query(
      "SELECT COUNT(t) FROM TimerSession t WHERE t.workspaceMemberId = :memberId AND t.isRunning = true")
  long countActiveByMemberId(@Param("memberId") UUID memberId);

  // where the :memberId is, that is where it will be replaced by the variable in the query

  // this function will first find the workspaces where the workspaceID is a member and then find if
  // there is a timer that is true
  Optional<TimerSession> findFirstByWorkspaceMemberIdInAndIsRunningTrue(
      List<UUID> workspaceMemberIds);
}
