package timesheets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.enums.EmploymentType;
import timesheets.enums.SeniorityLevel;
import timesheets.enums.UserStatus;

// this is the entity that represents the user in the database

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "job_title", length = 150)
  private String jobTitle;

  @Enumerated(EnumType.STRING)
  @Column(name = "seniority_level", length = 20)
  private SeniorityLevel seniorityLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "employment_type", length = 20)
  private EmploymentType employmentType;

  @Column(name = "email_verified")
  @Builder.Default
  private Boolean emailVerified = false;

  @Column(name = "failed_login_attempts", nullable = false)
  @Builder.Default
  private Integer failedLoginAttempts = 0;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "deletion_requested_at")
  private LocalDateTime deletionRequestedAt;

  @Column(name = "deletion_reason")
  private String deletionReason;

  @Column(name = "deletion_processed_at")
  private LocalDateTime deletionProcessedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "userId", fetch = FetchType.LAZY)
  @Builder.Default
  private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

  @OneToMany(mappedBy = "userId", fetch = FetchType.LAZY)
  @Builder.Default
  private List<UserIdentityProvider> identityProviders = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
