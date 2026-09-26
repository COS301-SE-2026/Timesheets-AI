/*
This handles the bridge between fastapi dashboard endpoint and spring bookt backend
its a rest client not webclient.

Author: Zamokuhle Zwane
Date: 02/09/2026

Patch: added includeResolved param to getDashboardInsights, added resolveInsight (V19)
Patch: added period param to getDashboardInsights (Developer Insights v2)
*/

package timesheets.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import timesheets.dto.response.AiDashboardResponse;
import timesheets.dto.response.ResolveInsightResponse;

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

  public AiDashboardResponse getDashboardInsights(UUID workspaceMemberId) {
    return getDashboardInsights(workspaceMemberId, false, "8w");
  }

  public AiDashboardResponse getDashboardInsights(UUID workspaceMemberId, boolean includeResolved) {
    return getDashboardInsights(workspaceMemberId, includeResolved, "8w");
  }

  public AiDashboardResponse getDashboardInsights(
      UUID workspaceMemberId, boolean includeResolved, String period) {
    return buildClient()
        .get()
        .uri(
            "/insights/dashboard/{workspaceMemberId}?include_resolved={includeResolved}&period={period}",
            workspaceMemberId,
            includeResolved,
            period)
        .retrieve()
        .body(new ParameterizedTypeReference<AiDashboardResponse>() {});
  }

  public ResolveInsightResponse resolveInsight(UUID insightId, UUID resolvedByWorkspaceMemberId) {
    return buildClient()
        .patch()
        .uri("/insights/{insightId}/resolve", insightId)
        .body(Map.of("resolved_by_workspace_member_id", resolvedByWorkspaceMemberId))
        .retrieve()
        .body(new ParameterizedTypeReference<ResolveInsightResponse>() {});
  }
}
