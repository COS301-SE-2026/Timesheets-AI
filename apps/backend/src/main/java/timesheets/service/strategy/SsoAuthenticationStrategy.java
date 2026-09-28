package timesheets.service.strategy;

/*
- this class is the strategy - the interface
-  each class will inherit from here
*/
public interface SsoAuthenticationStrategy {

  // finds the specific provider the strategy handles
  String getProvider();

  // gets the provider token and extracts a authenticated user
  SsoUserInfo authenticate(String idToken);
}
