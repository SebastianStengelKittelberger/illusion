package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@link Image} (media object) records from Elasticsearch.
 * Uses {@code search_after} pagination with {@code _doc} sort to handle large datasets.
 * Only active when {@code elasticsearch.enabled=true}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchMediaObjectLoadService {

  private static final int PAGE_SIZE = 500;

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;

  @Value("${elasticsearch.media-object-index:bosch-media-objects}")
  private String indexName;

  public ElasticsearchMediaObjectLoadService(
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
   * Loads all media objects from Elasticsearch.
   *
   * @return list of images, or {@code null} if the index does not exist (caller falls back to HTTP)
   */
  public List<Image> loadMediaObjects() {
    List<Image> result = new ArrayList<>();
    String searchAfterSortValue = null;

    while (true) {
      Map<String, Object> request = buildSearchRequest(searchAfterSortValue);
      JsonNode response;

      try {
        String requestJson = objectMapper.writeValueAsString(request);
        String responseJson = esHttpClient.post()
          .uri("/" + indexName + "/_search")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .body(requestJson)
          .retrieve()
          .body(String.class);
        response = objectMapper.readTree(responseJson);
      } catch (RestClientException e) {
        if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
          throw new IllegalStateException(
            "ES index '" + indexName + "' not found — run POST /{country}/{language}/index/media-objects on bosch.adapter first", e);
        }
        throw e;
      }

      JsonNode hits = response.path("hits").path("hits");
      if (!hits.isArray() || hits.isEmpty()) break;

      for (JsonNode hit : hits) {
        result.add(objectMapper.treeToValue(hit.path("_source"), Image.class));
      }

      searchAfterSortValue = hits.get(hits.size() - 1).path("sort").get(0).asText();
      if (hits.size() < PAGE_SIZE) break;
    }

    log.debug("Read {} media object(s) from ES index '{}'", result.size(), indexName);
    return result;
  }

  private Map<String, Object> buildSearchRequest(String searchAfterSortValue) {
    Map<String, Object> request = new HashMap<>();
    request.put("size", PAGE_SIZE);
    request.put("sort", List.of("_doc"));
    if (searchAfterSortValue != null) {
      request.put("search_after", List.of(Long.parseLong(searchAfterSortValue)));
    }
    return request;
  }
}
