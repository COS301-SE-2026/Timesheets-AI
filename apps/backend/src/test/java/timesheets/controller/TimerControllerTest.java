// package timesheets.controller;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import timesheets.domain.TimerSession;
// import timesheets.domain.TimeEntry;

// import timesheets.dto.request.StartTimerRequest;
// import timesheets.service.TimerService;
// import exception.ConflictException;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;

// import java.time.LocalDateTime;
// import java.util.UUID;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.*;

// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest(TimerController.class)
// class TimerControllerTest {

//     @Autowired
//     private MockMvc mockMvc; //this is how we will mock the HTTP requests

//     @Autowired
//     private ObjectMapper objectMapper;  //this will convert the startTimer request to JSON

//     @MockBean
//     private TimerService timerService;  //this is the fake service that we control

//     private UUID workspaceMemberId;
//     private UUID timerId;

//     private StartTimerRequest startRequest;

//     @BeforeEach
//     void setUp() {
//         workspaceMemberId = UUID.randomUUID();
//         timerId = UUID.randomUUID();

//         startRequest = new StartTimerRequest();

//         startRequest.setProjectId(UUID.randomUUID());
//         startRequest.setTaskId(UUID.randomUUID());
//         startRequest.setNotes("Test timer");
//     }

//     //this will test when a user starts a timer successfully
//     @Test
//     void startTimer_Success_ShouldReturn200WithTimer() throws Exception {

//         //Mockito: creates a fake timer to return when startTimer is called
//         TimerSession timer = new TimerSession();
//         timer.setId(timerId);

//         timer.setWorkspaceMemberId(workspaceMemberId);
//         timer.setStartedAt(LocalDateTime.now());
//         timer.setIsRunning(true);

//         // Mockito: "when someone calls startTimer with these parameters, return this fake timer"
//         when(timerService.startTimer(eq(workspaceMemberId),
// any(StartTimerRequest.class))).thenReturn(timer);

//         //SpringBoot: simulates an HTTP POST request to the controller
//         mockMvc.perform(post("/api/timers/start").header("X-Workspace-Member-Id",
// workspaceMemberId).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(startRequest)))
//
// .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(timerId.toString())).andExpect(jsonPath("$.active").value(true));

//         // mockMvc simulates an HTTP request, no need to start a real browser
//         // /api/timers/start is my request URL
//         // the header X-Workspace-Member-Id tells the backend which workspace member is making
// the request
//         // .andExpect() verifies if the response will be a 200
//     }

//     //this will test when a user tries to start a timer but already has one running
//     // it should return an HTTP 409 CONFLICT with an error message and the existing timer ID
//     @Test
//     void startTimer_Conflict_ShouldReturn409() throws Exception {

//         // Mockito: makes the service throw a ConflictException when startTimer is called
//         when(timerService.startTimer(eq(workspaceMemberId),
// any(StartTimerRequest.class))).thenThrow(new ConflictException("Timer already active", "You
// already have a running timer", timerId));

//         // SpringBoot: simulates an HTTP POST request to the controller
//         mockMvc.perform(post("/api/timers/start").header("X-Workspace-Member-Id",
// workspaceMemberId).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(startRequest))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("You already have a running timer")).andExpect(jsonPath("$.activeTimerId").value(timerId.toString()));
//         // .andExpect() verifies the actual response matches what we expect, we expect that 409
// conflict that I wrote
//     }

//     //this will test when a user stops a running timer
//     @Test
//     void stopTimer_Success_ShouldReturn200() throws Exception {

//         //Mockito: creates a fake time entry to return when stopTimer is called
//         TimeEntry timeEntry = new TimeEntry();
//         timeEntry.setId(UUID.randomUUID());

//         timeEntry.setDurationMinutes(60);
//         timeEntry.setEndTime(LocalDateTime.now());

//         //Mockito: "when someone calls stopTimer, return this fake time entry"
//         when(timerService.stopTimer(workspaceMemberId)).thenReturn(timeEntry);
//         when(timerService.getActiveTimer(workspaceMemberId)).thenReturn(null);

//         //SpringBoot: simulates an HTTP POST request to the controller
//         mockMvc.perform(post("/api/timers/stop").header("X-Workspace-Member-Id",
// workspaceMemberId)).andExpect(status().isOk());
//         // .andExpect() verifies the actual response matches the HTTP 200, that we want
//     }

//     //this will test when the frontend asks for the active timer and one is running
//     @Test
//     void getActiveTimer_WhenTimerExists_ShouldReturn200() throws Exception {

//         //Mockito: creates a fake timer to return when getActiveTimer is called
//         TimerSession timer = new TimerSession();
//         timer.setId(timerId);

//         timer.setIsRunning(true);
//         timer.setStartedAt(LocalDateTime.now().minusMinutes(30));

//         //Mockito: "when someone calls getActiveTimer, return this fake timer"
//         when(timerService.getActiveTimer(workspaceMemberId)).thenReturn(timer);

//         //SpringBoot: simulates an HTTP GET request to the controller
//         mockMvc.perform(get("/api/timers/active").header("X-Workspace-Member-Id",
// workspaceMemberId)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(timerId.toString())).andExpect(jsonPath("$.active").value(true));
//         // .andExpect() verifies the actual response matches the 200 that we expect
//     }

//     // this will test when the frontend asks for the active timer and none is running
//     // it should return an HTTP 204 NO CONTENT (no timer found)
//     @Test
//     void getActiveTimer_WhenNoTimer_ShouldReturn204() throws Exception {

//         //Mockito: simulates null, meaning that no active timer was found
//         when(timerService.getActiveTimer(workspaceMemberId)).thenReturn(null);

//         //SpringBoot: simulates an HTTP GET request to the controller
//         mockMvc.perform(get("/api/timers/active").header("X-Workspace-Member-Id",
// workspaceMemberId)).andExpect(status().isNoContent());
//         // .andExpect() verifies the actual response of a 204
//     }

//     // this will test when a user cancels a running timer, should return success since there is
// nothing to run
//     @Test
//     void discardTimer_Success_ShouldReturn204() throws Exception {

//         //Mockito: creates a fake timer to return when getActiveTimer is called
//         TimerSession timer = new TimerSession();
//         timer.setId(timerId);

//         //Mockito: "when someone calls getActiveTimer, return this fake timer"
//         when(timerService.getActiveTimer(workspaceMemberId)).thenReturn(timer);

//         // Mockito: "do nothing when discardTimer is called"
//         doNothing().when(timerService).discardTimer(workspaceMemberId);

//         // SpringBoot: simulates an HTTP DELETE request to the controller
//         mockMvc.perform(delete("/api/timers/discard")
//                 .header("X-Workspace-Member-Id", workspaceMemberId))
//                 .andExpect(status().isNoContent());

//         // mockMvc simulates an HTTP request, no need to start a real browser
//         // /api/timers/discard is my request URL
//         // the header X-Workspace-Member-Id tells the backend which workspace member is making
// the request
//         // .andExpect() verifies the actual response matches what we expect (HTTP 204)
//     }

//     //this will test when a user tries to discard a timer, but none is running
//     //it should return an HTTP 404 NOT FOUND
//     @Test
//     void discardTimer_NoActiveTimer_ShouldReturn404() throws Exception {

//         //Mockito: simulates null, meaning that a timer was not found
//         when(timerService.getActiveTimer(workspaceMemberId)).thenReturn(null);

//         //SpringBoot: simulates an HTTP DELETE request to the controller
//         mockMvc.perform(delete("/api/timers/discard").header("X-Workspace-Member-Id",
// workspaceMemberId)).andExpect(status().isNotFound());

//         // mockMvc simulates an HTTP request, no need to start a real browser
//         // /api/timers/discard is my request URL
//         // the header X-Workspace-Member-Id tells the backend which workspace member is making
// the request
//         // .andExpect() verifies the actual response matches what we expect (HTTP 404)
//     }
// }
