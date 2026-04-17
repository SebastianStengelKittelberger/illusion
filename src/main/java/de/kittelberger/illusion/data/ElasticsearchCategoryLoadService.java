package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Category;
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
 * Reads {@link Category} records from Elasticsearch.
 * Only active when {@code elasticsearch.enabled=true}.
 *
 * <p>Categories were previously indexed by bosch.adapter into
 * {@code {category-index-prefix}-{country}-{language}} via
 * {@code POST /{country}/{language}/index/categories}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchCategoryLoadService {

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;

  @Value("${elasticsearch.category-index-prefix:bosch-categories}")
  private String indexPrefix;

  @org.springframework.beans.factory.annotation.Autowired
  public ElasticsearchCategoryLoadService(
    ObjectMapper objectMapper,
    @Value("${elasticsearch.host:localhost}") String host,
    @Value("${elasticsearch.port:9200}") int port
  ) {
    this.esHttpClient = RestClient.builder()
      .baseUrl("http://" + host + ":" + port)
      .build();
    this.objectMapper = objectMapper;
  }

  /** Package-private constructor for testing — accepts a pre-configured {@link RestClient}. */
  ElasticsearchCategoryLoadService(ObjectMapper objectMapper, RestClient esHttpClient) {
    this.esHttpClient = esHttpClient;
    this.objectMapper = objectMapper;
  }

  /**
   * Loads all categories from Elasticsearch for the given locale.
   *
   * @throws IllegalStateException if the ES index does not exist (run adapter index endpoint first)
   */
  public List<Category> loadCategories(String country, String language) {
    String indexName = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();
    log.info("Reading categories from ES index '{}'", indexName);

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

      JsonNode hits = objectMapper.readTree(responseJson).path("hits").path("hits");
      if (!hits.isArray()) {
        throw new IllegalStateException("Unexpected ES response for index '" + indexName + "'");
      }

      List<Category> categories = new ArrayList<>();
      for (JsonNode hit : hits) {
        categories.add(objectMapper.treeToValue(hit.path("_source"), Category.class));
      }

      log.info("Read {} category/categories from ES index '{}'", categories.size(), indexName);
      return categories;

    } catch (RestClientException e) {
      if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
        throw new IllegalStateException(
          "ES index '" + indexName + "' not found — run POST /{country}/{language}/index/categories on bosch.adapter first", e);
      }
      throw e;
    }
  }
}
