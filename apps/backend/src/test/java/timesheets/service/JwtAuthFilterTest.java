package timesheets.service;

// intergration tests for the JwtAuthFilter to ensure that it correctly processes incoming HTTP
// requests
// extracts and validates JWT tokens, and sets the authentication in the security context as
// expected
// while also handling edge cases such as missing or malformed tokens, blacklisted tokens
// and exceptions during token processing, to verify that the filter behaves robustly and securely
// in various scenarios when integrated into the application's security configuration
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock JwtService jwtService;
  @Mock UserDetailsService userDetailsService;
  @Mock TokenBlacklistService tokenBlacklistService;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock FilterChain filterChain;
}
  // @InjectMocks JwtAuthFilter jwtAuthFilter;

  // private User user;

  // @BeforeEach
  // void setUp() { //clears the security context before each test to ensure that tests do not
  // interfere with each other by leaving authentication information in the context, providing a
  // clean slate for each test case
  //     SecurityContextHolder.clearContext();

  //     user = User.builder() //builds sample user
  //         .id(UUID.randomUUID())
  //         .email("bob@momentum.co.za")
  //         .passwordHash("hash")
  //         .status(UserStatus.ACTIVE)
  //         .emailVerified(true)
  //         .loginAttempts(0)
  //         .build();
  // }

  // @Test
  // void doesNotAuthenticate_whenNoAuthHeader() throws Exception {
  //     //this test checks that when the HTTP request does not contain an Authorization header,
  //     //the JwtAuthFilter's doFilterInternal method does not set any authentication in the
  // security
  //     when(request.getHeader("Authorization")).thenReturn(null);

  //     jwtAuthFilter.doFilterInternal(request, response, filterChain);

  //     verify(filterChain).doFilter(request, response);
  //     assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  // }

  // @Test
  // void doesNotAuthenticate_whenHeaderNotBearer() throws Exception {
  //     //this test checks that when the HTTP request contains an Authorization header that does
  // not start with "Bearer "(on swagger just press the lock icon and add the token without the
  // "Bearer " prefix),
  //     //the JwtAuthFilter's doFilterInternal method does not set any authentication in the
  // security
  //     when(request.getHeader("Authorization")).thenReturn("Basic abc123");

  //     jwtAuthFilter.doFilterInternal(request, response, filterChain);

  //     verify(filterChain).doFilter(request, response);
  //     assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  // }

  // @Test
  // void doesNotAuthenticate_whenTokenIsBlacklisted() throws Exception {
  //     //this test checks that when the HTTP request contains a Bearer token in the Authorization
  // header that is present in the TokenBlacklistService
  //     //the JwtAuthFilter's doFilterInternal method does not set any authentication in the
  // security context
  //     //ensuring that blacklisted tokens are properly rejected and do not grant access to
  // protected resources, while
  //     //still allowing the filter chain to continue processing the request without interruption
  //     when(request.getHeader("Authorization")).thenReturn("Bearer blacklisted-token");
  //     when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

  //     jwtAuthFilter.doFilterInternal(request, response, filterChain);

  //     verify(filterChain).doFilter(request, response);
  //     assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  // }

  // @Test
  // void authenticates_whenTokenIsValid() throws Exception {
  //     //this test checks that when the HTTP request contains a valid Bearer token in the
  // Authorization header that is not blacklisted
  //     //the JwtAuthFilter's doFilterInternal method correctly extracts the email from the token
  //     //loads the user details using the UserDetailsService, validates the token against the user
  // details
  //     // and sets the authentication in the security context with the correct user information
  //     //ensuring that valid tokens are properly processed to authenticate the user and grant
  // access to protected resources
  //     //while still allowing the filter chain to continue processing the request after
  // authentication is set
  //     String token = "valid-token";
  //     when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
  //     when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
  //     when(jwtService.extractEmail(token)).thenReturn("bob@momentum.co.za");
  //     when(userDetailsService.loadUserByUsername("bob@momentum.co.za")).thenReturn(user);
  //     when(jwtService.isTokenValid(token, "bob@momentum.co.za")).thenReturn(true);

  //     //jwtAuthFilter.doFilterInternal(request, response, filterChain);

  //     verify(filterChain).doFilter(request, response);
  //     assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
  //     assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
  //         .isEqualTo("bob@momentum.co.za");
  // }

  // @Test
  // void doesNotAuthenticate_whenTokenIsInvalid() throws Exception {
  //     //this test checks that when the HTTP request contains a Bearer token in the Authorization
  // header that is not blacklisted but fails validation,
  //     //the JwtAuthFilter's doFilterInternal method does not set any authentication in the
  // security
  //     //context, ensuring that invalid tokens are properly rejected and do not grant access to
  // protected resources, while
  //     //still allowing the filter chain to continue processing the request without interruption,
  // even when the token
  //     //is well-formed but fails validation checks (e.g., signature mismatch, expired token,
  // etc.)
  //     String token = "invalid-token";
  //     when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
  //     when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
  //     when(jwtService.extractEmail(token)).thenReturn("bob@momentum.co.za");
  //     when(userDetailsService.loadUserByUsername("bob@momentum.co.za")).thenReturn(user);
  //     when(jwtService.isTokenValid(token, "bob@momentum.co.za")).thenReturn(false);

  //     //jwtAuthFilter.doFilterInternal(request, response, filterChain);

  //     verify(filterChain).doFilter(request, response);
  //     assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  // }

  // @Test
  // void continuesChain_whenExceptionThrown() throws Exception {
  //     //this test checks that when the HTTP request contains a Bearer token in the Authorization
  // header
  //     //that is not blacklisted but an exception is thrown during token processing (e.g., during
  // email extraction),
  //     //the JwtAuthFilter's doFilterInternal method does not set any authentication in the
  // security
  //     //context and allows the filter chain to continue processing the request without
  // interruption, ensuring that unexpected errors during token
  //     String token = "bad-token";
  //     when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
  //     when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
  //     when(jwtService.extractEmail(token)).thenThrow(new RuntimeException("parse error"));

  //     jwtAuthFilter.doFilterInternal(request, response, filterChain);

  //     verify(filterChain).doFilter(request, response);
  //     assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  // }
