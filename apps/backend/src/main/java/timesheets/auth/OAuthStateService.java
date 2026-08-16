/*
The system is stateless. There are no sessions to use and store nonce  
What happended before?
- wroking on the endpoint, I wanted store the state that has been generated and use that to later on verify that it is the same so we can eventually store it in the integration_tokens table 
But we are not using sessions so I need to make Stateless OAuth state 

state should have workspace member ID, random value and expiration
- it will be short lived security value
GOAL here: angular connect to google calender, do the normal authentication and GoogleOAuthService generate a state and user see consent screen by Google and user accepts and then 
redirect begins here. In this callback, we will have code and state and the GoogleOAuthController will verify state and GoogleOuthService will exchange the code and save it into the table. 
to verify state, I will make the state a signed value similar to JwtService.java 

Avoiding using the jwtService because I want to keep it separate - the JWT represents that the user has been authenticated to Timesheets-AI and 
OAuth state represents that the specific OAuth authorization attempt was intiated by this workspace member. 

OAuthStateService will:
 - generate state
 - verify state
*/