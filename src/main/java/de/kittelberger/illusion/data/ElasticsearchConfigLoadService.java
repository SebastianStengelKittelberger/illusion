package de.kittelberger.illusion.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads configuration values (e.g. domain) from the {@code bosch-config} Elasticsearch index.
 * Only active when {@code elasticsearch.enabled=true}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchConfigLoadService {

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;

  @Value("${elasticsearch.config-index:bosch-config}")
  private String indexName;

  public ElasticsearchConfigLoadService(
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
   * Reads the media domain string from Elasticsearch.
   *
   * @return the domain value, or {@code null} if not found in ES (caller falls back to HTTP)
   */
  public String loadDomain() {
    try {
      String responseJson = esHttpClient.get()
        .uri("/" + indexName + "/_doc/domain")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(String.class);

      JsonNode node = objectMapper.readTree(responseJson);
      String value = node.path("_source").path("value").textValue();
      log.debug("Read domain '{}' from ES index '{}'", value, indexName);
      return value;
    } catch (RestClientException e) {
      if (e.getMessage() != null &&
        (e.getMessage().contains("index_not_found") || e.getMessage().contains("404"))) {
        throw new IllegalStateException(
          "Domain config not found in ES index '" + indexName + "' — run POST /index/domain on bosch.adapter first", e);
      }
      throw e;
    }
  }
}
