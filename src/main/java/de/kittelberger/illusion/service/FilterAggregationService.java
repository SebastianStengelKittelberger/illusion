package de.kittelberger.illusion.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import de.kittelberger.illusion.model.FilterConfig;
import de.kittelberger.illusion.model.FilterValueCount;
import de.kittelberger.illusion.model.MapConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.*;

/**
 * Aggregates filter values across all products of a category.
 *
 * <p>Reads the SKU list from {@code illusion-categories-{c}-{l}}, then executes an
 * Elasticsearch {@code terms} aggregation on {@code filters.*} fields for those SKUs,
 * returning a map of {@code UKEY -> [{value, count}, ...]}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class FilterAggregationService {

  private final RestClient esHttpClient;
  private final ObjectMapper objectMapper;
  private final String indexPrefix;

  public FilterAggregationService(
    RestClient elasticsearchRestClient,
    ObjectMapper objectMapper,
    @Value("${elasticsearch.index-prefix:illusion}") String indexPrefix
  ) {
    this.esHttpClient = elasticsearchRestClient;
    this.objectMapper = objectMapper;
    this.indexPrefix = indexPrefix;
  }

  /**
   * Returns aggregated filter values for all products belonging to the given category.
   *
   * @param country       country code (e.g. "de")
   * @param language      language code (e.g. "de")
   * @param categoryUkey  category ukey (ES document ID)
   * @param mapConfigs    all mapping configs; only those with enabled FilterConfig are used
   * @return map of UKEY to sorted list of value/count pairs (descending by count)
   */
  public Map<String, List<FilterValueCount>> aggregate(
    String country,
    String language,
    String categoryUkey,
    List<MapConfig> mapConfigs
  ) {
    List<String> skus = loadCategorySkus(country, language, categoryUkey);
    if (skus.isEmpty()) {
      return Map.of();
    }

    List<String> filterUkeys = mapConfigs.stream()
      .filter(c -> c.getFilterConfig() != null && c.getFilterConfig().isEnabled())
      .map(MapConfig::getUkey)
      .toList();

    if (filterUkeys.isEmpty()) {
      return Map.of();
    }

    return runAggregation(country, language, skus, filterUkeys);
  }

  private List<String> loadCategorySkus(String country, String language, String categoryUkey) {
    String catIndex = indexPrefix + "-categories-" + country.toLowerCase() + "-" + language.toLowerCase();
    try {
      String json = esHttpClient.get()
        .uri("/" + catIndex + "/_doc/" + categoryUkey)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(String.class);

      JsonNode skusNode = objectMapper.readTree(json).path("_source").path("skus");
      List<String> skus = new ArrayList<>();
      if (skusNode.isArray()) {
        skusNode.forEach(n -> skus.add(n.asText()));
      }
      return skus;
    } catch (Exception e) {
      log.warn("Could not load SKUs for category '{}': {}", categoryUkey, e.getMessage());
      return List.of();
    }
  }

  private Map<String, List<FilterValueCount>> runAggregation(
    String country,
    String language,
    List<String> skus,
    List<String> filterUkeys
  ) {
    String productIndex = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();

    // Build aggs block: one terms agg per filter UKEY
    Map<String, Object> aggs = new LinkedHashMap<>();
    for (String ukey : filterUkeys) {
      aggs.put(ukey, Map.of("terms", Map.of("field", "filters." + ukey + ".keyword", "size", 100)));
    }

    Map<String, Object> query = Map.of(
      "query", Map.of("terms", Map.of("sku", skus)),
      "aggs", aggs,
      "size", 0
    );

    try {
      String responseJson = esHttpClient.post()
        .uri("/" + productIndex + "/_search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(objectMapper.writeValueAsString(query))
        .retrieve()
        .body(String.class);

      return parseAggregations(objectMapper.readTree(responseJson).path("aggregations"), filterUkeys);
    } catch (Exception e) {
      log.error("Filter aggregation failed for {}/{}: {}", country, language, e.getMessage());
      return Map.of();
    }
  }

  private Map<String, List<FilterValueCount>> parseAggregations(JsonNode aggsNode, List<String> filterUkeys) {
    Map<String, List<FilterValueCount>> result = new LinkedHashMap<>();
    for (String ukey : filterUkeys) {
      JsonNode buckets = aggsNode.path(ukey).path("buckets");
      if (buckets.isArray() && !buckets.isEmpty()) {
        List<FilterValueCount> values = new ArrayList<>();
        buckets.forEach(b -> values.add(new FilterValueCount(b.path("key").asText(), b.path("doc_count").asLong())));
        result.put(ukey, values);
      }
    }
    return result;
  }
}
