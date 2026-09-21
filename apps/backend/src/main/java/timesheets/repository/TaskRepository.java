package timesheets.repository;

import java.time.LocalDateTime;
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

  // when a member leaves their workspace, the unfinished tasks get unassigned so they can be
  // reassigned
  // completed tasks will still have the assignee so we can see who completed what task
  @Modifying
  @Query(
      "UPDATE Task t "
          + "SET t.assignedWorkspaceMemberId = NULL, t.updatedAt = :updatedAt "
          + "WHERE t.assignedWorkspaceMemberId = :workspaceMemberId "
          + "AND t.isDeleted = false "
          + "AND t.status <> 'DONE'")
  void unassignActiveTasksFromWorkspaceMember(
      @Param("workspaceMemberId") UUID workspaceMemberId,
      @Param("updatedAt") LocalDateTime updatedAt);

  // when a member leaves a project, their unfinished tasks are unassigned
  @Modifying
  @Query(
      "UPDATE Task t "
          + "SET t.assignedWorkspaceMemberId = NULL, t.updatedAt = :updatedAt "
          + "WHERE t.projectId = :projectId "
          + "AND t.assignedWorkspaceMemberId = :workspaceMemberId "
          + "AND t.isDeleted = false "
          + "AND t.status <> 'DONE'")
  void unassignActiveTasksFromProjectMember(
      @Param("projectId") UUID projectId,
      @Param("workspaceMemberId") UUID workspaceMemberId,
      @Param("updatedAt") LocalDateTime updatedAt);

  // when a Jira issue changes, this will update the local task with the latest details from Jira
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

  // finds all non-deleted tasks belonging to projects in a specific workspace
  @Query(
      "SELECT t FROM Task t "
          + "JOIN Project p ON t.projectId = p.id "
          + "WHERE p.workspaceId = :workspaceId "
          + "AND t.isDeleted = false")
  List<Task> findActiveTasksByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
