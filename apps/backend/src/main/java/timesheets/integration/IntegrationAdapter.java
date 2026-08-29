package timesheets.integration;

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
}
