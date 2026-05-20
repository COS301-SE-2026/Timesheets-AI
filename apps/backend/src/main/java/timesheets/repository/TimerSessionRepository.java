package timesheets.repository;

import timesheets.domain.TimerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// okay so I am going to use the method of allowing Spring to parse through the method names to get an SQL query
// I like it, it's faster and somewhat easier, but I will use SQL here and there yk??

@Repository
public interface TimerSessionRepository extends JpaRepository<TimerSession, UUID> {
    
    //should find an active timer of a member by their workspace_id
    Optional<TimerSession> findByWorkspaceMemberIdAndIsRunningTrue(UUID workspaceMemberId);
    
    
    //sees if the user has an active timer
    boolean existsByWorkspaceMemberIdAndIsRunningTrue(UUID workspaceMemberId);
    
    
    //this will check the active timer of a user, it can't be more than 1
    @Query("SELECT COUNT(t) FROM TimerSession t WHERE t.workspaceMemberId = :memberId AND t.isRunning = true")
    long countActiveByMemberId(@Param("memberId") UUID memberId);

    //where the :memberId is, that is where it will be replaced by the variable in the query
}