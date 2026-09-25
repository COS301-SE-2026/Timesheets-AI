/*
This handles the bridge between fastapi dashboard endpoint and spring bookt backend
its a rest client not webclient.

Author: Zamokuhle Zwane
Date: 02/09/2026

Updated: Nyasha
date 25/09/2026
*/

package timesheets.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import timesheets.dto.response.AiDashboardResponse;
import timesheets.dto.response.ProjectForecastResponse;
import timesheets.dto.response.SavedProjectForecastResponse;

@Component
@RequiredArgsConstructor
public class AiServiceClient {
  private final RestClient.Builder restClientBuilder;

  @Value("${app.ai-service.url}")
  private String aiServiceBaseUrl;

  // fastapi returns snake cause while spring uses camelcase
  private final JsonMapper snakeCaseMapper =
      JsonMapper.builder()
          .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .addModule(new JavaTimeModule())
          .build();

  public AiDashboardResponse getDashboardInsights(UUID workspaceMemberId) {
    RestClient client =
        restClientBuilder
            .baseUrl(aiServiceBaseUrl)
            .messageConverters(
                converters -> {
                  converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                  converters.add(new MappingJackson2HttpMessageConverter(snakeCaseMapper));
                })
            .build();

    return client
        .get()
        .uri("/insights/dashboard/{workspaceMemberId}", workspaceMemberId)
        .retrieve()
        .body(new ParameterizedTypeReference<AiDashboardResponse>() {});
  }

  /*
  - gets the recent project from the ai service
  - allows springboot to expose the last synced forcast to the frontend
  - returned response has the saved forecast and the last sync
   */
  public SavedProjectForecastResponse getProjectForecast(UUID projectId) {
    RestClient client = buildClient();

    return client
        .get()
        .uri("/insights/project-forecast/{projectId}", projectId)
        .retrieve()
        .body(new ParameterizedTypeReference<SavedProjectForecastResponse>() {});
  }

  public ProjectForecastResponse syncProjectForecast(UUID projectId, String authorization) {

    RestClient client = buildClient();

    return client
        .post()
        .uri("/insights/project-forecast/{projectId}/sync", projectId)
        .header("Authorization", authorization)
        .retrieve()
        .body(new ParameterizedTypeReference<ProjectForecastResponse>() {});
  }

  

  /*
  - used the style that Zamo originally did
  - this creates the client to communicate with the ai service
  - also makes sure that the snake fields returned by python as mapped into the java response dto
   */
  private RestClient buildClient() {
    return restClientBuilder
        .baseUrl(aiServiceBaseUrl)
        .messageConverters(
            converters -> {
              converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
              converters.add(new MappingJackson2HttpMessageConverter(snakeCaseMapper));
            })
        .build();
  }
}
