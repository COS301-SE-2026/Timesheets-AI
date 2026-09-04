package timesheets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import timesheets.domain.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

  // finds all the tasks belonging to a specific project, including the deleted ones
  List<Task> findByProjectId(UUID projectId);

  // finds all the tasks on a project, that are not deleted - think showing all the tasks on a
  // project board??
  List<Task> findByProjectIdAndIsDeletedFalse(UUID projectId);

  // finds all the tasks for a specific member, and the tasks are active - think showing "my tasks"
  List<Task> findByAssignedWorkspaceMemberIdAndIsDeletedFalse(UUID workspaceMemberId);

  // this checks if a project has any active tasks
  boolean existsByProjectIdAndIsDeletedFalse(UUID projectId);

  // this should be for finding a task by its Jira ticket id
  Optional<Task> findByJiraTicketKey(String jiraTicketKey);

  // this should find all the tasks that are linked to Jira issues
  List<Task> findByJiraTicketKeyIsNotNull();

  // this will find all the tasks linked to Jira issues that are not deleted
  List<Task> findByJiraTicketKeyIsNotNullAndIsDeletedFalse();

  @Modifying
  @Query(
      "UPDATE Task t SET t.status = :status, t.title = :title, t.description = :description, t.dueDate = :dueDate, t.updatedAt = :updatedAt WHERE t.id = :taskId")
  void syncFromJira(
      @Param("taskId") UUID taskId,
      @Param("status") String status,
      @Param("title") String title,
      @Param("description") String description,
      @Param("dueDate") java.time.LocalDate dueDate,
      @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
