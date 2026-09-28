package timesheets.service.strategy;

/*
- this will be what the app needs when the SSO provider has authenticated a user
- since both return different token structs, the strategy pattern I am implementing will convert the data in the specific format
*/
public record SsoUserInfo(
    String provider,
    String providerUserId,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    boolean emailVerified) {}
