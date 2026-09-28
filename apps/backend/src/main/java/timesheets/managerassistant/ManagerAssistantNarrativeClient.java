/*
This file handle bridges to ai-service for the one sentence of gemini generated narrative, mirrors AiServiceClient.getDashboardInsights's setup exactly, same
restclient + snake_case mapper, just a different fastapi route

gemini lives in ai-service (python), never in this spring app, same split thedashboard insights already use, keeping the boundary consistent across the
whole codebase not just this feature

this is the ONLY class in this package allowed to fail loudly without sinking the whole feature. if ai-service is down or gemini's free tier quota is hit,
we fall back to a template sentence instead of throwing a 500 at the manager mid review

Author: Zamokuhle Zwane
Date: 20 September 2026
*/

package timesheets.managerassistant;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManagerAssistantNarrativeClient {

  private final RestClient.Builder restClientBuilder;

  @Value("${app.ai-service.url}")
  private String aiServiceBaseUrl;

  private final JsonMapper snakeCaseMapper =
      JsonMapper.builder()
          .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .addModule(new JavaTimeModule())
          .build();

  // matches ManagerAssistantNarrativeResponse on the ai-service side, a bare {"narrative": "..."}
  // object, not a raw string body
  private record NarrativeResponse(String narrative) {}

  public String getNarrative(ManagerAssistantReview review) {
    try {
      RestClient client =
          restClientBuilder
              .baseUrl(aiServiceBaseUrl)
              .messageConverters(
                  converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(snakeCaseMapper));
                  })
              .build();

      NarrativeResponse response =
          client
              .post()
              .uri("/insights/manager-assistant/narrative")
              .body(review)
              .retrieve()
              .body(NarrativeResponse.class);

      return response.narrative();
    } catch (Exception e) {
      // ai-service down, gemini quota hit, whatever it is, log it and fall back rather than let a
      // narrative failure break the whole
      // evidence review the manager is waiting on
      log.warn("gemini narrative call failed, falling back to templated sentence", e);
      return buildFallbackNarrative(review);
    }
  }

  // plain conditional string building, only hit when gemini isn't available, this is a safety net
  // not the main path so keeping it dumb on purpose
  private String buildFallbackNarrative(ManagerAssistantReview review) {
    if (review.hasMissingEvidence()) {
      return "No GitHub commits, Jira tickets, or calendar events were found "
          + "matching this period. The manager should review this entry directly.";
    }
    if (review.hasConflict()) {
      return "This entry overlaps with a calendar event during logged development "
          + "time. Worth checking with the developer before approving.";
    }
    if (review.confidenceScorePercent() >= 75) {
      return "The timesheet aligns with the available evidence for this period. "
          + "No inconsistencies were found.";
    }
    return "The available evidence only partially supports this timesheet entry. "
        + "Manual review recommended.";
  }
}
