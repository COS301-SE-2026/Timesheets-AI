package timesheets.config;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// this is where we set up our security configuration for the backend, using Spring Security
// we define how users are authenticated and authorized to access different endpoints in our API
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${app.cors.allowed-origins:http://localhost:4200}")
  private String[] allowedOrigins;

  @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
  private String[] allowedMethods;

  @Value("${app.cors.allowed-headers:*}")
  private String[] allowedHeaders;

  @Value("${app.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Bean // this is how we encode passwords, using BCrypt which is a strong hashing
  // algorithm to
  // securely store user passwords in the database
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * This method makes sure: 1. CORS: Allows cross-origin requests from the Angular frontend 2.
   * CSRF: Disabled because we use stateless JWT tokens 3. Session: Stateless (no server-side
   * session storage) 4. Authorization: public endpoints are accessible, protect everything else 5.
   * JWT Filter: Validates JWT tokens for authenticated requests
   *
   * <p>The filter chain executes in this order: 1. CORS filter (handles preflight OPTIONS requests)
   * 2. CSRF filter (disabled) 3. JWT Authentication Filter (validates token) 4. Authorization
   * filter (checks permissions)
   */
  @Bean // this is where we configure the security filter chain, defining how HTTP
  // requests are
  // secured and which endpoints require authentication
  public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        // update to match what was set prior in application.yml:
                        //  refer to springdoc.api-docs.path=/v3/api-docs
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/google",
                        "/api/auth/verify/**",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/actuator/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            jwtAuthFilter,
            UsernamePasswordAuthenticationFilter
                .class); // this adds our custom JWT authentication filter
    // to the security filter
    // chain, ensuring that incoming requests are checked for valid JWT tokens
    // before allowing access to protected endpoints
    return http.build();
  }

  /*
  CORS configuration for cross-origin requests.
  - Needing this since angular frontend runs on a different origin (http://localhost:4200) than the backend (http://localhost:8080)
  - the config details that are allowed are:

      - Allowed Origins: http://localhost:4200 (Angular dev server)
      - Allowed Methods: All standard HTTP methods for REST APIs
      - Allowed Headers: All headers (including Authorization for JWT)
      - Allow Credentials: true (allows cookies and auth headers)
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration configuration = new CorsConfiguration();

    // from the angular frontend
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

    // the options is specifically for CORS preflight requests
    configuration.setAllowedMethods(Arrays.asList(allowedMethods));

    configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
    configuration.setAllowCredentials(allowCredentials);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // it is applied to all endpoints such that the CORS handling can occur across the entire API
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
