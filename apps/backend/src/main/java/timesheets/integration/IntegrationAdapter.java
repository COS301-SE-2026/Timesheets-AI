package timesheets.integration;

// every integration adapter MUST follow this contract 

public interface IntegrationAdapter {
    // every adapter need to tell us which external provider do they represent?
    String getProvider();
}