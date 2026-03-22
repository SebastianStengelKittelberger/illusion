package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Product;
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
import java.util.function.Predicate;

/**
 * Reads raw {@link Product} records from Elasticsearch using {@code search_after} pagination.
 * Only active when {@code elasticsearch.enabled=true}.
 *
 * <p>Products were previously indexed by bosch.adapter into
 * {@code {product-index-prefix}-{country}-{language}} via
 * {@code POST /{country}/{language}/index}.
 *
 * <p>Uses Spring's {@link RestClient} to call the ES REST API directly, which allows using
 * the same Jackson 3 {@link ObjectMapper} that is used everywhere else in illusion,
 * avoiding any cross-version serialization issues.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchProductLoadService {

  private static final int PAGE_SIZE = 100;

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;

  @Value("${elasticsearch.product-index-prefix:bosch-products}")
  private String indexPrefix;

  public ElasticsearchProductLoadService(
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
   * Streams all products for the given locale from Elasticsearch, calling {@code consumer} for each.
   * Paginates using {@code search_after} on {@code _doc} to keep memory usage constant.
   *
   * @throws IllegalStateException if the ES index does not exist (run adapter index endpoint first)
   */
  public void streamProducts(String country, String language, Predicate<Product> consumer) {
    String indexName = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();
    log.info("Reading products from ES index '{}'", indexName);

    String lastId = null;
    int totalRead = 0;

    while (true) {
      Map<String, Object> request = buildSearchRequest(lastId);
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
            "ES index '" + indexName + "' not found — run POST /{country}/{language}/index on bosch.adapter first", e);
        }
        throw e;
      }

      JsonNode hits = response.path("hits").path("hits");
      if (!hits.isArray() || hits.isEmpty()) break;

      for (JsonNode hit : hits) {
        Product product = objectMapper.treeToValue(hit.path("_source"), Product.class);
        if (!consumer.test(product)) return;
      }

      totalRead += hits.size();
      lastId = hits.get(hits.size() - 1).path("sort").get(0).asText();

      if (hits.size() < PAGE_SIZE) break;
    }

    log.info("Read {} product(s) from ES index '{}'", totalRead, indexName);
  }

  private Map<String, Object> buildSearchRequest(String searchAfterSortValue) {
    Map<String, Object> request = new HashMap<>();
    request.put("size", PAGE_SIZE);
    // _doc sort is the most efficient and always available — no fielddata needed
    request.put("sort", List.of("_doc"));
    if (searchAfterSortValue != null) {
      request.put("search_after", List.of(Long.parseLong(searchAfterSortValue)));
    }
    return request;
  }
}
