package timesheets.repository;

import timesheets.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    
    //this will see if a certain member is already assigned to a specific project
    boolean existsByProjectIdAndWorkspaceMemberId(UUID projectId, UUID workspaceMemberId);
}