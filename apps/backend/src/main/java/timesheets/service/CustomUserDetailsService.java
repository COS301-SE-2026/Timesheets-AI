package timesheets.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.enums.UserStatus;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  @Override
  public UserDetails loadUserByUsername(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    List<GrantedAuthority> authorities = new ArrayList<>();
    List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());

    for (WorkspaceMember membership : memberships) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + membership.getRole().name()));

      authorities.add(new SimpleGrantedAuthority("WORKSPACE_ " + membership.getWorkspaceId()));
    }

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPasswordHash(),
        user.getStatus() == UserStatus.ACTIVE && Boolean.TRUE.equals(user.getEmailVerified()),
        true,
        true,
        !(user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())),
        authorities);
  }
}

/*1. spring security calls loadByUsername the email
2. the service finds the user in the DB
3. loads the workspace memberships - their roles
4. builds a spring security user object with the email, password, the roles and workspace permissions, and the account account status
5. spring security will then use that to authenticate the user

the two true, true means that the account and credential are not expired*/
