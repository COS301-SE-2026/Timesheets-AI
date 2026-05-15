package timesheets.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


//just to customize the title and description on how the document looks
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Timesheets AI API")
                .version("1.0")
                .description("API for time tracking, timesheet approvals, and productivity insights")
                .contact(new Contact()
                    .name("Cybernauts")
                    .email("Cybernauts301@gmail.com")));
    }
}

//check out the swagger docs at: http://localhost:8080/swagger-ui/index.html