package timesheets.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

  /*all the tasks belonging to a specific project (found by ID), will be gotten and stored in the list*/
  List<Task> findByProjectId(UUID projectId);
}
