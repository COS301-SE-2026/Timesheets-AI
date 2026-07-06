// package timesheets.domain;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;
// import java.time.LocalDateTime;
// import java.util.UUID;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// // this is the entity that will be stored in the database for email verification tokens.
// @Entity // this annotation tells Spring Boot that this class is an entity that should be stored
// in
// // the database
// @Table(name = "email_verification_tokens")
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class EmailVerificationToken {
//   @Id private String token;

//   @Column(name = "user_id", nullable = false)
//   private UUID userId;

//   @Column(name = "expires_at", nullable = false)
//   private LocalDateTime expiresAt;

//   @Column(nullable = false)
//   @Builder.Default
//   private Boolean verified = false;
// }
