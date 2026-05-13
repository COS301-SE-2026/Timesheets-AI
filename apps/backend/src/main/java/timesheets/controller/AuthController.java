package timesheets.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.dto.request.LoginRequest;
import timesheets.dto.request.RegisterRequest;
import timesheets.dto.response.AuthResponse;
import timesheets.dto.response.UserResponse;
import timesheets.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login and logout endpoints")

//This class is responsible for handling authentication-related requests,
//  such as user registration, login, and logout.
//  It uses the AuthService to perform the necessary operations and returns appropriate responses based on the outcome of each request.
public class AuthController {
    private final AuthService authService;

    @Operation(
        summary = "Register a new user/employee",
        description = "Registers a new user/employee with the provided details.",   
        responses= {
             @ApiResponse(responseCode = "201", description = "User created",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
        }
    )
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        return new ResponseEntitystatus(HttpStatus.CREATED).body(userResponse);
    }

    @Operation(
        summary = "Login a user/employee",
        description = "Authenticates a user/employee and returns an authentication token.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
        }
    )
    @PostMapping("/login")
    public ResponseEntity <AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }
        @Operation(
            summary = "Logout a user/employee",
            description = "Logs out the currently authenticated user/employee.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Logout successful"),
                @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
            }
        )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}