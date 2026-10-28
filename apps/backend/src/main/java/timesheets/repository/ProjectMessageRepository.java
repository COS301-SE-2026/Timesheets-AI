package timesheets.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.ProjectMessage;

@Repository
public interface ProjectMessageRepository extends JpaRepository<ProjectMessage, UUID> {

  // latest 200 messages for a project, newest first (the service reverses them)
  List<ProjectMessage> findTop200ByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
