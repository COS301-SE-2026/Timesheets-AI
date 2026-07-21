package timesheets.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/*
- this is a helper class to get the logged-in user info from the security context
- there won't be any DB queries since it will be stored in memory
*/
@Component
@RequiredArgsConstructor
public class SecurityUtils {

  // gets the authenticated user details
  public CustomUserDetails getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new RuntimeException("user not authenticated");
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof CustomUserDetails) {
      return (CustomUserDetails) principal;
    }

    throw new RuntimeException("Invalid authentication principal. Expected CustomUserDetails");
  }

  // user info
  public UUID getCurrentUserId() {
    return getCurrentUser().getUserId();
  }

  public String getCurrentEmail() {
    return getCurrentUser().getEmail();
  }

  public String getCurrentUserFirstName() {
    return getCurrentUser().getFirstName();
  }

  public String getCurrentLastName() {
    return getCurrentUser().getLastName();
  }

  public String getCurrentUserFullName() {
    return getCurrentUser().getFullName();
  }

  // workspace checks
  // gets the default workspace member id from the security context
  public UUID getDefaultWorkspaceMemberId() {
    return getCurrentUser().getDefaultWorkspaceMemberId();
  }

  public boolean belongsToWorkspace(UUID workspaceId) {
    return getCurrentUser().belongsToWorkspace(workspaceId);
  }

  public long getWorkspaceCount() {
    return getCurrentUser().getWorkspaceCount();
  }

  public UUID getCurrentWorkspaceId() {
    return getCurrentUser().getWorkspaceId();
  }

  // role checks
  public boolean hasRole(String role) {
    return getCurrentUser().hasRole(role);
  }

  public boolean isAdmin() {
    return getCurrentUser().isAdmin();
  }

  public boolean isManager() {
    return getCurrentUser().isManager();
  }

  public boolean isDeveloper() {
    return getCurrentUser().isDeveloper();
  }

  public boolean isUser() {
    return getCurrentUser().isUser();
  }

  // auth stuff
  public Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  public boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication.getPrincipal() instanceof String);
  }
}
