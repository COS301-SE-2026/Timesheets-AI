package timesheets.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import timesheets.domain.SuggestedWorkSessionEntity;

public interface SuggestionRepository extends JpaRepository<SuggestedWorkSessionEntity, UUID> {

  List<SuggestedWorkSessionEntity> findByWorkspaceMemberIdOrderByStartTimeDesc(
      UUID workspaceMemberId);

  List<SuggestedWorkSessionEntity> findByWorkspaceMemberIdAndStatus(
      UUID workspaceMemberId, String status);

  List<SuggestedWorkSessionEntity> findByWorkspaceMemberId(UUID workspaceMemberId);

  boolean existsByWorkspaceMemberIdAndStartTimeAndEndTimeAndTitle(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime, String title);
}
