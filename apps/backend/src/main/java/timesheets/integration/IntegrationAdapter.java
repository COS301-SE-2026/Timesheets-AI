package timesheets.integration;

import java.util.UUID;

// TODO:
/*
 * every integration need to:
 * generate specific authorization URL
 * handle its specific token exchnage and identity
 */
// every integration adapter MUST follow this contract

public interface IntegrationAdapter {
  // every adapter need to tell us which external provider do they represent?
  String getProvider();

  // every vendor must execute their token and save it to the DB d
  void exchangeAndsaveToken(UUID workspaceMemberId, String code);
}
