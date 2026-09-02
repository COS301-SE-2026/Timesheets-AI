package timesheets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.Workspace;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

  // Find workspace by name (case insensitive)

  /*
  - this will find a workspace by the name, and ignore the case
  - want to prevent workspaces having duplicate names
  - preventing something like "tech" vs "Tech" */
  Optional<Workspace> findByNameIgnoreCase(String name);

  // finding the workspaces for a specified user
  List<Workspace> findByOwnerUserId(UUID ownerUserId);

  // seeing if the workspace with a specific name exists
  boolean existsByNameIgnoreCase(String name);
}
