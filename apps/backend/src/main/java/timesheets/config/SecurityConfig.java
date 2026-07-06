// package timesheets.config;

// import lombok.RequiredArgsConstructor;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import timesheets.repository.UserRepository;

// // this is where we set up our security configuration for the backend, using Spring Security
// // we define how users are authenticated and authorized to access different endpoints in our API
// @Configuration
// @EnableWebSecurity
// @RequiredArgsConstructor
// public class SecurityConfig {

//   private final UserRepository userRepository;

//   @Bean // this is how we load user details for authentication, using the UserRepository to find
//   // users by email
//   public UserDetailsService userDetailsService() {
//     return username ->
//         userRepository
//             .findByEmail(username)
//             .orElseThrow(() -> new UsernameNotFoundException("user not found: " + username));
//   }

//   @Bean // this is how we encode passwords, using BCrypt which is a strong hashing algorithm to
//   // securely store user passwords in the database
//   public PasswordEncoder passwordEncoder() {
//     return new BCryptPasswordEncoder();
//   }

//   @Bean // this is where we configure the security filter chain, defining how HTTP requests are
//   // secured and which endpoints require authentication
//   public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter)
//       throws Exception {
//     http.csrf(csrf -> csrf.disable())
//         .sessionManagement(
//             session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//         .authorizeHttpRequests(
//             auth ->
//                 auth.requestMatchers(
//                         "/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**")
//                     .permitAll()
//                     .anyRequest()
//                     .authenticated())
//         .addFilterBefore(
//             jwtAuthFilter,
//             UsernamePasswordAuthenticationFilter
//                 .class); // this adds our custom JWT authentication filter to the security filter
//     // chain, ensuring that incoming requests are checked for valid JWT tokens
//     // before allowing access to protected endpoints
//     return http.build();
//   }
// }
