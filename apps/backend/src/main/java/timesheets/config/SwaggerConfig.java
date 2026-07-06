// package timesheets.config;

// import io.swagger.v3.oas.models.Components;
// import io.swagger.v3.oas.models.OpenAPI;
// import io.swagger.v3.oas.models.info.Contact;
// import io.swagger.v3.oas.models.info.Info;
// import io.swagger.v3.oas.models.security.SecurityRequirement;
// import io.swagger.v3.oas.models.security.SecurityScheme;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class SwaggerConfig {

//   @Bean
//   public OpenAPI customOpenAPI() {
//     return new OpenAPI()
//         .info(
//             new Info()
//                 .title("Timesheets AI API")
//                 .version("1.0")
//                 .description(
//                     "API for time tracking, timesheet approvals, and productivity insights")
//                 .contact(new Contact().name("Cybernauts").email("Cybernauts301@gmail.com")))
//         .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
//         .components(
//             new Components()
//                 .addSecuritySchemes(
//                     "bearerAuth",
//                     new SecurityScheme()
//                         .name("bearerAuth")
//                         .type(SecurityScheme.Type.HTTP)
//                         .scheme("bearer")
//                         .bearerFormat("JWT")));
//   }
// }

// // check out the swagger docs at: http://localhost:8080/swagger-ui/index.html
