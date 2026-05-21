package timesheets.repository;

import timesheets.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    //all the tasks belonging to a specific project (found by ID), will be gotten and stored in the list 
    List<Task> findByProjectId(UUID projectId);
}