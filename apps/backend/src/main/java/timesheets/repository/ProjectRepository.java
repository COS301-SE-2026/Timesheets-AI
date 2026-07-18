package timesheets.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

  // finds alll the project in a workspace that has not been soft deleted
  List<Project> findByWorkspaceIdAndIsDeletedFalse(UUID workspaceId);

  // finds all the projects for the workspace, this will also be the soft deleted ones
  List<Project> findByWorkspaceId(UUID workspaceId);
}
