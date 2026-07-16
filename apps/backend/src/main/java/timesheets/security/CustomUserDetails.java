package timesheets.security;

import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/*
- so I are trying to prevent needing DB queries for user details on every request
- creating a class that will stores additional info beyond what Spring User has
*/

@Getter
public class CustomUserDetails extends User {
  private final UUID userId;
  private final String email;
  private final String firstName;
  private final String lastName;
  private final Boolean emailVerified;
  private final UUID defaultWorkspaceMemberId;
  private final Collection<GrantedAuthority> authorities;

  public CustomUserDetails(
      UUID userId,
      String email,
      String firstName,
      String lastName,
      Boolean emailVerified,
      UUID defaultWorkspaceMemberId,
      String passwordHash,
      boolean enabled,
      boolean accountNonExpired,
      boolean credentialsNonExpired,
      boolean accountNonLocked,
      Collection<GrantedAuthority> authorities) {

    // the email is the username
    super(
        email,
        passwordHash,
        enabled,
        accountNonExpired,
        credentialsNonExpired,
        accountNonExpired,
        authorities);

    this.userId = userId;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.emailVerified = emailVerified;
    this.defaultWorkspaceMemberId = defaultWorkspaceMemberId;
    this.authorities = authorities;
  }

  // get the full name
  public String getFullName() {
    return firstName + " " + lastName;
  }

  // checks if the user has a specific role
  public boolean hasRole(String role) {
    if (authorities == null) return false;

    return authorities.stream().anyMatch(authority -> authority.getAuthority().equals(role));
  }

  // ! helper functions for checking the role
  public boolean isAdmin() {
    return hasRole("ROLE_ADMIN");
  }

  public boolean isManager() {
    return hasRole("ROLE_MANAGER");
  }

  public boolean isDeveloper() {
    return hasRole("ROLE_DEVELOPER");
  }

  public boolean isUser() {
    return hasRole("ROLE_USER");
  }

  // ! helper fucntions for the workspace
  // will check if the user belongs to any workspace
  public boolean belongsToWorkspace(UUID workspaceId) {
    if (authorities == null || workspaceId == null) return false;

    return authorities.stream()
        .anyMatch(authority -> authority.getAuthority().equals("WORKSPACE_" + workspaceId));
  }

  public long getWorkspaceCount() {
    if (authorities == null) return 0;

    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("WORKSPACE_"))
        .count();
  }
}
