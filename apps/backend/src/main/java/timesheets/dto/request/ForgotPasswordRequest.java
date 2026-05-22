package timesheets.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//this is the quest object for the forgot password endpoint,
//it only needs the email of the user who forgot their password
//might not be used for demo 1 i'm not sure if frontend is adding it
@Data
public class ForgotPasswordRequest {
    
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    private String email;
}