package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

//links a user to a specific workspace with a specific role

@Entity
@Table(name = "workspace_members")
public class WorkspaceMember {

    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workspace_role_id")
    private Integer workspaceRoleId;


    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }



    public UUID getWorkspaceId() {
        return workspaceId;
    }


    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getUserId() {
        return userId;
    }


    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getWorkspaceRoleId() {
        return workspaceRoleId;
    }

    public void setWorkspaceRoleId(Integer workspaceRoleId) {
        this.workspaceRoleId = workspaceRoleId;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }


    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public UUID getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(UUID lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}