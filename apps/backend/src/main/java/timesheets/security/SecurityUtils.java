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
}
