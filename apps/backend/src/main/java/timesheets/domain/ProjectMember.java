package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

//this will be the entity to match the user to a specific project, and also show their role within that project

@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_member_id", nullable = false)
    private UUID workspaceMemberId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "project_role_id")
    private Integer projectRoleId;

    @Column(name = "allocation_percentage")
    private Integer allocationPercentage;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

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

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceMemberId() {
        return workspaceMemberId;
    }

    public void setWorkspaceMemberId(UUID workspaceMemberId) {
        this.workspaceMemberId = workspaceMemberId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public Integer getProjectRoleId() {
        return projectRoleId;
    }

    public void setProjectRoleId(Integer projectRoleId) {
        this.projectRoleId = projectRoleId;
    }

    public Integer getAllocationPercentage() {
        return allocationPercentage;
    }

    public void setAllocationPercentage(Integer allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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