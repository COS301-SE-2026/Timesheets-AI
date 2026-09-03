/*
This handles the bridge between fastapi dashboard endpoint and spring bookt backend
its a rest client not webclient.

Author: Zamokuhle Zwane
Date: 02/09/2026
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
}
