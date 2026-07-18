package timesheets.dto.response;

import lombok.Data;

// this is going to represent the user data returned from Google's OAuth2 token validation
@Data
public class GoogleUserInfoResponse {
  private String googleUserId;
  private String email;
  private String fullName;
  private String firstName;
  private String lastName;
  private String avatarUrl;
  private boolean emailVerified;
}
