package timesheets.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.JiraTicket;

@Repository
public interface JiraTicketRepository extends JpaRepository<JiraTicket, UUID> {
  // jira_tickets has no workspace_member_id column (checked V15's CREATE TABLE),
  // only project_id, so tickets belonging to a specific dev are found by
  // matching jira_ticket_key against that dev's Task rows, same join
  // IntegrationController.getJiraVsLogged() already does
  List<JiraTicket> findByJiraTicketKeyIn(List<String> jiraTicketKeys);
}
