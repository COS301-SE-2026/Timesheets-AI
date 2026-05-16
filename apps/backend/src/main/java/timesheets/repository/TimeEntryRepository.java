package timesheets.repository;

import timesheets.domain.TimeEntry;
import timesheets.enums.TimeEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

//this file helps with functions for the database, such that the functions interact with the database without writing SQL 
//so what I did here is I just gave the decription of the functions, Spring Boot will be the one that creates the actual functions at runtime


@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    
    List<TimeEntry> findByWorkspaceMemberIdOrderByStartTimeDesc(Long workspaceMemberId);
    
    List<TimeEntry> findByWorkspaceMemberIdAndStatus(Long workspaceMemberId, TimeEntryStatus status);
    
    List<TimeEntry> findByWorkspaceMemberIdAndStartTimeBetween(Long workspaceMemberId, LocalDateTime start, LocalDateTime end);
    
    List<TimeEntry> findByStatus(TimeEntryStatus status);
}