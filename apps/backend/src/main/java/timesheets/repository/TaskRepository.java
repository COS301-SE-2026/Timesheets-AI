package timesheets.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

  //finds all the tasks belonging to a specific project, including the deleted ones
  List<Task> findByProjectId(UUID projectId);

  //finds all the tasks on a project, that are not deleted - think showing all the tasks on a project board??
  List<Task> findByProjectIdAndIsDeletedFalse(UUID projectId);

  //finds all the tasks for a specific member, and the tasks are active - think showing "my tasks"
  List<Task> findByAssignedWorkspaceMemberIdAndIsDeletedFalse(UUID workspaceMemberId);

  //this checks if a project has any active tasks
  boolean existsByProjectIdAndIsDeletedFalse(UUID projectId);
}
