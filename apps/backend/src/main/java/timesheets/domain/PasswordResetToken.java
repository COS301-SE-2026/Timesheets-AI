// package timesheets.domain;

// import jakarta.persistence.*;
// import java.time.LocalDateTime;
// import java.util.UUID;
// import lombok.*;

// // this is the entity that will be stored in the database for password reset tokens.
// @Entity // this annotation tells Spring Boot that this class is an entity that should be stored
// in
// // the database
// @Table(name = "password_reset_tokens")
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class PasswordResetToken {

//   @Id private String token;

//   @Column(name = "user_id", nullable = false)
//   private UUID userId;

//   @Column(name = "expires_at", nullable = false)
//   private LocalDateTime expiresAt;

//   @Column(nullable = false)
//   @Builder.Default
//   private Boolean used = false;
// }
