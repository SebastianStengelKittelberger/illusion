package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Reference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@link Reference} records from Elasticsearch.
 * Only active when {@code elasticsearch.enabled=true}.
 *
 * <p>References were previously indexed by bosch.adapter into
 * {@code {reference-index-prefix}-{country}-{language}} via
 * {@code POST /{country}/{language}/index/references}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchReferenceLoadService {

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;

  @Value("${elasticsearch.reference-index-prefix:bosch-references}")
  private String indexPrefix;

  public ElasticsearchReferenceLoadService(
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
   * Loads all references from Elasticsearch for the given locale.
   *
   * @return the list of references, or {@code null} if the index does not exist (caller falls back to HTTP)
   */
  public List<Reference> loadReferences(String country, String language) {
    String indexName = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();

    Map<String, Object> request = new HashMap<>();
    request.put("size", 10000);

    try {
      String requestJson = objectMapper.writeValueAsString(request);
      String responseJson = esHttpClient.post()
        .uri("/" + indexName + "/_search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(requestJson)
        .retrieve()
        .body(String.class);

      JsonNode responseNode = objectMapper.readTree(responseJson);
      JsonNode hits = responseNode.path("hits").path("hits");

      if (!hits.isArray()) throw new IllegalStateException("Unexpected ES response for index '" + indexName + "'");

      List<Reference> references = new ArrayList<>();
      for (JsonNode hit : hits) {
        references.add(objectMapper.treeToValue(hit.path("_source"), Reference.class));
      }

      log.debug("Read {} reference(s) from ES index '{}'", references.size(), indexName);
      return references;

    } catch (RestClientException e) {
      if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
        throw new IllegalStateException(
          "ES index '" + indexName + "' not found — run POST /{country}/{language}/index/references on bosch.adapter first", e);
      }
      throw e;
    }
  }
}
