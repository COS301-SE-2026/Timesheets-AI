package timesheets.repository;

import timesheets.domain.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    
    //this will find all the workspace memberships for a given user, thinking it can be used for like a drop down and stuff
    List<WorkspaceMember> findAllByUserId(UUID userId);

    

    //finds a specific membership with workspace membership and userID
    Optional<WorkspaceMember> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);

    
    //this will see if a user belongs to a specific workspace
    boolean existsByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);

    
    //this finds the members of a specific workspace, regardless of the role
    List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId);



    //! I am assuming that the role_ids are admin = 1, manager = 2, dev = 3. kinda like priority??

    //this should find all the developers, within a specific workspace
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspaceId = :workspaceId AND wm.workspaceRoleId = 3")
    List<WorkspaceMember> findAllDevelopersByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    //this should find all the managers within a specific workspace
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspaceId = :workspaceId AND wm.workspaceRoleId = 2")
    List<WorkspaceMember> findAllManagersByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    //this should find all the admins within a specific workspace
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspaceId = :workspaceId AND wm.workspaceRoleId = 1")
    List<WorkspaceMember> findAllAdminsByWorkspaceId(@Param("workspaceId") UUID workspaceId);



    //this will find if a specific user is locked from a workspace, is locked means they are suspended from a workspace, or that they cannot access it
    @Query("SELECT wm.isLocked FROM WorkspaceMember wm WHERE wm.userId = :userId AND wm.workspaceId = :workspaceId")
    Optional<Boolean> isUserLockedInWorkspace(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId);
}