package timesheets.config;
//I created this filter to intercept incoming HTTP requests and check for a valid JWT token in the Authorization header
//If a valid token is found, it sets the authentication in the Spring Security context so that
//the user is authenticated for the duration of the request.
//It also checks if the token is blacklisted (e.g. after logout) and ignores it if so.

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import timesheets.service.JwtService;
import timesheets.service.TokenBlacklistService;

import java.io.IOException;

//filter for JWT authentication, checks for valid token and sets authentication in security context if valid
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    //intercept each request and check for valid JWT token
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); //7 is length of "Bearer "

        if (tokenBlacklistService.isBlacklisted(token)) { //'blacklist' check to prevent use of tokens that have been invalidated (e.g. on logout)
            filterChain.doFilter(request, response);
            return;
        }
        //validate token and set authentication if valid
        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {//check if token is valid for the user
                var userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, email)) { //if valid, set authentication in security context
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); //set details from request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // invalid token - just continue unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}
