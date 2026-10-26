package timesheets.service;

import exception.AccessDeniedException;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Project;
import timesheets.domain.ProjectMember;
import timesheets.domain.ProjectMessage;
import timesheets.domain.ProjectMessageReaction;
import timesheets.domain.User;
import timesheets.dto.response.ProjectMessageLocationResponse;
import timesheets.dto.response.ProjectMessageResponse;
import timesheets.dto.response.ProjectMessageResponse.ReactionSummary;
import timesheets.enums.ReactionType;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectMessageReactionRepository;
import timesheets.repository.ProjectMessageRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

// the project community: one channel per project, with messages, reactions and @mentions
@Service
@RequiredArgsConstructor
public class ProjectCommunityService {

  private static final String MENTION_NOTIFICATION = "PROJECT_MENTION";
  private static final String MESSAGE_ENTITY = "PROJECT_MESSAGE";

  private final ProjectMessageRepository messageRepository;
  private final ProjectMessageReactionRepository reactionRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;
  private final ProjectService projectService;
  private final NotificationService notificationService;
  private final SecurityUtils securityUtils;

  // latest messages for the project, oldest first so the UI can render top to bottom
  @Transactional(readOnly = true)
  public List<ProjectMessageResponse> getMessages(UUID projectId) {
    UUID me = securityUtils.getDefaultWorkspaceMemberId();
    requireAccess(projectId, me);

    List<ProjectMessage> ordered =
        new ArrayList<>(messageRepository.findTop200ByProjectIdOrderByCreatedAtDesc(projectId));
    Collections.reverse(ordered);

    return toResponses(ordered, me);
  }

  @Transactional
  public ProjectMessageResponse sendMessage(UUID projectId, String rawContent) {
    UUID me = securityUtils.getDefaultWorkspaceMemberId();
    Project project = requireAccess(projectId, me);

    String content = rawContent == null ? "" : rawContent.trim();
    if (content.isEmpty()) {
      throw new BadRequestException("Message cannot be empty");
    }

    ProjectMessage saved =
        messageRepository.save(
            ProjectMessage.builder()
                .projectId(projectId)
                .workspaceMemberId(me)
                .content(content)
                .build());

    notifyMentions(project, saved, me);

    return toResponses(List.of(saved), me).get(0);
  }

  // authors can delete their own messages, admins can delete any
  @Transactional
  public void deleteMessage(UUID projectId, UUID messageId) {
    UUID me = securityUtils.getDefaultWorkspaceMemberId();
    requireAccess(projectId, me);

    ProjectMessage message = findInProject(projectId, messageId);

    if (!message.getWorkspaceMemberId().equals(me) && !securityUtils.isAdmin()) {
      throw new AccessDeniedException("You can only delete your own messages");
    }

    messageRepository.delete(message);
  }

  // adds the reaction if I haven't reacted with it yet, otherwise removes it
  @Transactional
  public ProjectMessageResponse toggleReaction(
      UUID projectId, UUID messageId, ReactionType reactionType) {
    UUID me = securityUtils.getDefaultWorkspaceMemberId();
    requireAccess(projectId, me);

    ProjectMessage message = findInProject(projectId, messageId);

    Optional<ProjectMessageReaction> existing =
        reactionRepository.findByMessageIdAndWorkspaceMemberIdAndReactionType(
            messageId, me, reactionType);

    if (existing.isPresent()) {
      reactionRepository.delete(existing.get());
    } else {
      reactionRepository.save(
          ProjectMessageReaction.builder()
              .messageId(messageId)
              .workspaceMemberId(me)
              .reactionType(reactionType)
              .build());
    }

    return toResponses(List.of(message), me).get(0);
  }

  // lets a notification work out which project page to open
  @Transactional(readOnly = true)
  public ProjectMessageLocationResponse getMessageLocation(UUID messageId) {
    UUID me = securityUtils.getDefaultWorkspaceMemberId();

    ProjectMessage message =
        messageRepository
            .findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

    requireAccess(message.getProjectId(), me);

    return new ProjectMessageLocationResponse(message.getProjectId(), message.getId());
  }

  // helpers

  private Project requireAccess(UUID projectId, UUID workspaceMemberId) {
    Project project =
        projectRepository
            .findById(projectId)
            .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

    if (!projectService.userHasAccessToProject(projectId, workspaceMemberId)) {
      throw new AccessDeniedException("No access to this project");
    }

    return project;
  }

  private ProjectMessage findInProject(UUID projectId, UUID messageId) {
    ProjectMessage message =
        messageRepository
            .findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

    if (!message.getProjectId().equals(projectId)) {
      throw new ResourceNotFoundException("Message not found");
    }

    return message;
  }

  // a mention is "@First Last" (case-insensitive) matching an active project member
  private void notifyMentions(Project project, ProjectMessage message, UUID authorMemberId) {
    if (!message.getContent().contains("@")) {
      return;
    }

    List<UUID> memberIds =
        projectMemberRepository.findByProjectIdAndIsActiveTrue(project.getId()).stream()
            .map(ProjectMember::getWorkspaceMemberId)
            .filter(id -> !id.equals(authorMemberId))
            .toList();

    if (memberIds.isEmpty()) {
      return;
    }

    Map<UUID, String> names = fullNames(memberIds);
    String authorName = fullNames(List.of(authorMemberId)).getOrDefault(authorMemberId, "Someone");

    for (UUID memberId : memberIds) {
      String name = names.get(memberId);
      if (name == null || name.isBlank()) {
        continue;
      }

      Pattern mention =
          Pattern.compile("@" + Pattern.quote(name) + "(?!\\w)", Pattern.CASE_INSENSITIVE);

      if (mention.matcher(message.getContent()).find()) {
        notificationService.createNotification(
            memberId,
            MENTION_NOTIFICATION,
            authorName + " mentioned you in " + project.getName(),
            snippet(message.getContent()),
            MESSAGE_ENTITY,
            message.getId());
      }
    }
  }

  private String snippet(String content) {
    return content.length() <= 120 ? content : content.substring(0, 117) + "...";
  }

  private List<ProjectMessageResponse> toResponses(List<ProjectMessage> messages, UUID me) {
    if (messages.isEmpty()) {
      return List.of();
    }

    List<UUID> ids = messages.stream().map(ProjectMessage::getId).toList();

    Map<UUID, List<ProjectMessageReaction>> reactionsByMessage =
        reactionRepository.findByMessageIdIn(ids).stream()
            .collect(Collectors.groupingBy(ProjectMessageReaction::getMessageId));

    Map<UUID, String> names =
        fullNames(
            messages.stream()
                .map(ProjectMessage::getWorkspaceMemberId)
                .collect(Collectors.toSet()));

    return messages.stream()
        .map(
            message ->
                ProjectMessageResponse.builder()
                    .id(message.getId())
                    .projectId(message.getProjectId())
                    .authorWorkspaceMemberId(message.getWorkspaceMemberId())
                    .authorName(names.getOrDefault(message.getWorkspaceMemberId(), "Former member"))
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .updatedAt(message.getUpdatedAt())
                    .mine(message.getWorkspaceMemberId().equals(me))
                    .reactions(
                        summarise(reactionsByMessage.getOrDefault(message.getId(), List.of()), me))
                    .build())
        .toList();
  }

  // one summary per reaction type that has at least one reaction, in a fixed order
  private List<ReactionSummary> summarise(List<ProjectMessageReaction> reactions, UUID me) {
    List<ReactionSummary> summaries = new ArrayList<>();

    for (ReactionType type : ReactionType.values()) {
      List<ProjectMessageReaction> ofType =
          reactions.stream().filter(r -> r.getReactionType() == type).toList();

      if (!ofType.isEmpty()) {
        boolean reactedByMe = ofType.stream().anyMatch(r -> r.getWorkspaceMemberId().equals(me));
        summaries.add(new ReactionSummary(type, ofType.size(), reactedByMe));
      }
    }

    return summaries;
  }

  // workspace member id -> "First Last"
  private Map<UUID, String> fullNames(Collection<UUID> workspaceMemberIds) {
    Map<UUID, UUID> userIdByMember = new HashMap<>();
    workspaceMemberRepository
        .findAllById(workspaceMemberIds)
        .forEach(member -> userIdByMember.put(member.getId(), member.getUserId()));

    Map<UUID, User> usersById =
        userRepository.findAllById(userIdByMember.values()).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

    Map<UUID, String> names = new HashMap<>();
    userIdByMember.forEach(
        (memberId, userId) -> {
          User user = usersById.get(userId);
          if (user != null) {
            names.put(memberId, (user.getFirstName() + " " + user.getLastName()).trim());
          }
        });

    return names;
  }
}
