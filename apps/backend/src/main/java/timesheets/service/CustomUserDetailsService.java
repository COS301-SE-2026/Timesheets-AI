package timesheets.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.enums.UserStatus;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    List<GrantedAuthority> authorities = new ArrayList<>();
    List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());

    UUID defaultWorkspaceMemberId = null;
    for (WorkspaceMember membership : memberships) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + membership.getRole().name()));

      authorities.add(new SimpleGrantedAuthority("WORKSPACE_" + membership.getWorkspaceId()));

      if (defaultWorkspaceMemberId == null) {
        defaultWorkspaceMemberId = membership.getWorkspaceId();
      }
    }

    return new CustomUserDetails(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmailVerified(),
        defaultWorkspaceMemberId,
        user.getPasswordHash(),
        user.getStatus() == UserStatus.ACTIVE && Boolean.TRUE.equals(user.getEmailVerified()),
        true,
        true,
        !(user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())),
        authorities);
  }
}

/*
1. spring security calls loadByUsername with the email
2. the service finds the user in the DB
3. loads the workspace memberships - their roles
4. builds the customobject user object with the email, password, the roles and workspace permissions, and the account account status
5. spring security will then use that to authenticate the user
6. the user info is now in the security context so no need to keep doing DB queries

the two true, true means that the account and credential are not expired*/
