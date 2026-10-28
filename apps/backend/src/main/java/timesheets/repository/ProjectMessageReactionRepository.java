package timesheets.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.ProjectMessageReaction;
import timesheets.enums.ReactionType;

@Repository
public interface ProjectMessageReactionRepository
    extends JpaRepository<ProjectMessageReaction, UUID> {

  // loads the reactions for a whole page of messages in one query
  List<ProjectMessageReaction> findByMessageIdIn(Collection<UUID> messageIds);

  Optional<ProjectMessageReaction> findByMessageIdAndWorkspaceMemberIdAndReactionType(
      UUID messageId, UUID workspaceMemberId, ReactionType reactionType);
}
