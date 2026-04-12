package de.kittelberger.illusion.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

/**
 * Persists and loads filter labels in the {@code illusion-labels} Elasticsearch index.
 * Labels are stored as a flat map of ukey → display label per country/language.
 * The same index can later be reused for other label namespaces in Illusion.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchLabelService {

  private static final String INDEX_NAME = "illusion-labels";
  private static final String NAMESPACE_FILTER = "filter";

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;

  public ElasticsearchLabelService(ObjectMapper objectMapper, RestClient elasticsearchRestClient) {
    this.objectMapper = objectMapper;
    this.esHttpClient = elasticsearchRestClient;
  }

  /** Loads the most recently saved filter labels for the given locale, or an empty map. */
  public Map<String, String> loadFilterLabels(String country, String language) {
    return load(NAMESPACE_FILTER, country, language);
  }

  /** Saves filter labels for the given locale (versioned by timestamp). */
  public void saveFilterLabels(String country, String language, Map<String, String> labels) {
    save(NAMESPACE_FILTER, country, language, labels);
  }

  // ── Generic namespace helpers ─────────────────────────────────────────────

  private Map<String, String> load(String namespace, String country, String language) {
    Map<String, Object> query = Map.of(
      "query", Map.of("bool", Map.of("filter", java.util.List.of(
        Map.of("term", Map.of("namespace.keyword", namespace)),
        Map.of("term", Map.of("country.keyword", country)),
        Map.of("term", Map.of("language.keyword", language))
      ))),
      "sort", java.util.List.of(Map.of("timestamp", "desc")),
      "size", 1
    );

    try {
      String json = esHttpClient.post()
        .uri("/" + INDEX_NAME + "/_search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(objectMapper.writeValueAsString(query))
        .retrieve()
        .body(String.class);

      JsonNode hits = objectMapper.readTree(json).path("hits").path("hits");
      if (!hits.isArray() || hits.isEmpty()) {
        log.info("No {} labels found in ES for {}/{}", namespace, country, language);
        return Map.of();
      }

      Map<String, String> labels = objectMapper.treeToValue(
        hits.get(0).path("_source").path("labels"),
        new TypeReference<>() {}
      );
      log.info("Loaded {} {} label(s) from ES for {}/{}", labels.size(), namespace, country, language);
      return labels;

    } catch (RestClientException e) {
      if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
        log.info("Labels index '{}' does not exist yet — returning empty map", INDEX_NAME);
        return Map.of();
      }
      throw e;
    }
  }

  private void save(String namespace, String country, String language, Map<String, String> labels) {
    Map<String, Object> doc = Map.of(
      "namespace", namespace,
      "country", country,
      "language", language,
      "timestamp", Instant.now().toString(),
      "labels", labels
    );

    esHttpClient.post()
      .uri("/" + INDEX_NAME + "/_doc")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .body(objectMapper.writeValueAsString(doc))
      .retrieve()
      .body(String.class);

    log.info("Saved {} {} label(s) to ES for {}/{}", labels.size(), namespace, country, language);
  }
}
