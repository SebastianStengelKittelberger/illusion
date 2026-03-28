package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.MapConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Persists and loads {@link MapConfig} lists in the {@code illusion-mapping-config} Elasticsearch index.
 * Each save writes a new document with a timestamp; loading retrieves the most recent entry
 * for the given country/language combination.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchMappingConfigService {

  private static final String INDEX_NAME = "illusion-mapping-config";

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;

  public ElasticsearchMappingConfigService(
    ObjectMapper objectMapper,
    @Value("${elasticsearch.host:localhost}") String host,
    @Value("${elasticsearch.port:9200}") int port
  ) {
    this.esHttpClient = RestClient.builder()
      .baseUrl("http://" + host + ":" + port)
      .build();
    this.objectMapper = objectMapper;
  }

  /**
   * Returns the most recently saved list of {@link MapConfig}s for the given locale,
   * or an empty list if none have been saved yet.
   */
  public List<MapConfig> loadLatest(String country, String language) {
    Map<String, Object> query = Map.of(
      "query", Map.of("bool", Map.of("filter", List.of(
        Map.of("term", Map.of("country.keyword", country)),
        Map.of("term", Map.of("language.keyword", language))
      ))),
      "sort", List.of(Map.of("timestamp", "desc")),
      "size", 1
    );

    try {
      String responseJson = esHttpClient.post()
        .uri("/" + INDEX_NAME + "/_search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(objectMapper.writeValueAsString(query))
        .retrieve()
        .body(String.class);

      JsonNode hits = objectMapper.readTree(responseJson).path("hits").path("hits");
      if (!hits.isArray() || hits.isEmpty()) {
        log.info("No mapping config found in ES for {}/{}", country, language);
        return List.of();
      }

      JsonNode configsNode = hits.get(0).path("_source").path("configs");
      List<MapConfig> configs = objectMapper.treeToValue(configsNode, new TypeReference<>() {});
      log.info("Loaded {} mapping config(s) from ES for {}/{}", configs.size(), country, language);
      return configs;

    } catch (RestClientException e) {
      if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
        log.info("Mapping config index '{}' does not exist yet — returning empty list", INDEX_NAME);
        return List.of();
      }
      throw e;
    }
  }

  /**
   * Saves the given {@link MapConfig} list as a new versioned document with the current timestamp.
   */
  public void save(String country, String language, List<MapConfig> configs) {
    Map<String, Object> doc = Map.of(
      "country", country,
      "language", language,
      "timestamp", Instant.now().toString(),
      "configs", configs
    );

    esHttpClient.post()
      .uri("/" + INDEX_NAME + "/_doc")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .body(objectMapper.writeValueAsString(doc))
      .retrieve()
      .body(String.class);

    log.info("Saved {} mapping config(s) to ES for {}/{}", configs.size(), country, language);
  }
}
