package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.data.ElasticsearchMappingConfigService;
import de.kittelberger.illusion.model.FilterValueCount;
import de.kittelberger.illusion.service.FilterAggregationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Returns aggregated filter values for all products in a category.
 *
 * <p>Example response:
 * <pre>{@code
 * {
 *   "VOLTAGE":  [{"value":"18V","count":8}, {"value":"12V","count":4}],
 *   "PERFORMANCECLASS": [{"value":"expert","count":5}]
 * }
 * }</pre>
 */
@RestController
public class FilterAggregationController {

  private final Optional<FilterAggregationService> aggregationService;
  private final Optional<ElasticsearchMappingConfigService> mappingConfigService;

  public FilterAggregationController(
    Optional<FilterAggregationService> aggregationService,
    Optional<ElasticsearchMappingConfigService> mappingConfigService
  ) {
    this.aggregationService = aggregationService;
    this.mappingConfigService = mappingConfigService;
  }

  @GetMapping("/{country}/{language}/category-{categoryUkey}/filter-aggregations")
  public Map<String, List<FilterValueCount>> getFilterAggregations(
    @PathVariable String country,
    @PathVariable String language,
    @PathVariable String categoryUkey
  ) {
    var service = aggregationService.orElseThrow(
      () -> new IllegalStateException("Elasticsearch not enabled — set elasticsearch.enabled=true"));
    var configs = mappingConfigService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch not enabled"))
      .loadLatest(country, language);

    return service.aggregate(country, language, categoryUkey, configs);
  }
}
