// package timesheets.dto.response;

// import java.time.LocalDateTime;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// // this is the response object for authentication, which contains the JWT token,
// // its expiration time, and some basic user info.

// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class AuthResponse {
//   private String token;
//   private LocalDateTime expiresAt; // we can set this to 1 hour for now
//   private UserInfo user;

//   @Data
//   @Builder
//   @NoArgsConstructor
//   @AllArgsConstructor
//   public static class UserInfo {
//     private String id;
//     private String email;
//     private String firstName;
//     private String lastName;
//   }
// }
