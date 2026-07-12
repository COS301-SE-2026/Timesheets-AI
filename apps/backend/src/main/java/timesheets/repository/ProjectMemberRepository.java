package timesheets.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.ProjectMember;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

  // this will see if a certain member is already assigned to a specific project
  boolean existsByProjectIdAndWorkspaceMemberId(UUID projectId, UUID workspaceMemberId);
}
