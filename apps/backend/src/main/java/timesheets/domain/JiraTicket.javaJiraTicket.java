/*
Read-only JPA mapping for jira_tickets (added in V15's seed migration).
No repository write methods on purpose - this table is populated by the
Jira sync (JiraAdapter/JiraOAuthService), not by this app.

Author: Zamokuhle Zwane
Date: 26/09/2026
*/

package timesheets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jira_tickets")
@Data
@NoArgsConstructor
public class JiraTicket {

  @Id private UUID id;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "jira_ticket_key", nullable = false)
  private String jiraTicketKey;

  private String summary;

  @Column(name = "jira_status")
  private String jiraStatus; // TODO / IN_PROGRESS / DONE / BLOCKED per the V15 seed values

  @Column(name = "issue_type")
  private String issueType;

  @Column(name = "estimated_hours")
  private BigDecimal estimatedHours;

  @Column(name = "logged_hours")
  private BigDecimal loggedHours;

  @Column(name = "last_synced")
  private LocalDateTime lastSynced;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}