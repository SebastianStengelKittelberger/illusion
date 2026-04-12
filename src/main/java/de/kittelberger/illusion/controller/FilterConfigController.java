package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.data.ElasticsearchLabelService;
import de.kittelberger.illusion.data.ElasticsearchMappingConfigService;
import de.kittelberger.illusion.model.FilterConfigEntry;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Returns all active filter configurations for a given locale,
 * merged with their display labels from the illusion-labels index.
 */
@RestController
public class FilterConfigController {

  private final Optional<ElasticsearchMappingConfigService> mappingConfigService;
  private final Optional<ElasticsearchLabelService> labelService;

  public FilterConfigController(
    Optional<ElasticsearchMappingConfigService> mappingConfigService,
    Optional<ElasticsearchLabelService> labelService
  ) {
    this.mappingConfigService = mappingConfigService;
    this.labelService = labelService;
  }

  /**
   * Returns all MapConfig entries that have an enabled FilterConfig,
   * enriched with their display label.
   */
  @GetMapping("/{country}/{language}/filter-config")
  public List<FilterConfigEntry> getFilterConfig(
    @PathVariable String country,
    @PathVariable String language
  ) {
    var mappingConfigs = mappingConfigService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .loadLatest(country, language);

    Map<String, String> labels = labelService
      .map(s -> s.loadFilterLabels(country, language))
      .orElse(Map.of());

    return mappingConfigs.stream()
      .filter(c -> c.getFilterConfig() != null && c.getFilterConfig().isEnabled())
      .map(c -> FilterConfigEntry.builder()
        .ukey(c.getUkey())
        .targetField(c.getTargetField())
        .filterConfig(c.getFilterConfig())
        .label(labels.getOrDefault(c.getUkey(), c.getUkey()))
        .build())
      .sorted(java.util.Comparator.comparingInt(e ->
        e.getFilterConfig().getOrder() != null ? e.getFilterConfig().getOrder() : Integer.MAX_VALUE))
      .toList();
  }
}
