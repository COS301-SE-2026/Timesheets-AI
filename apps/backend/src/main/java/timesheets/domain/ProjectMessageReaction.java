package timesheets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.enums.ReactionType;

// one workspace member's reaction on one message (a member can add each type once)
@Entity
@Table(name = "project_message_reactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMessageReaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "message_id", nullable = false)
  private UUID messageId;

  @Column(name = "workspace_member_id", nullable = false)
  private UUID workspaceMemberId;

  @Enumerated(EnumType.STRING)
  @Column(name = "reaction_type", nullable = false, length = 20)
  private ReactionType reactionType;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
