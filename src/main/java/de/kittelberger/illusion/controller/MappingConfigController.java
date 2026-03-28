package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.data.ElasticsearchMappingConfigService;
import de.kittelberger.illusion.model.MapConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class MappingConfigController {

  private final Optional<ElasticsearchMappingConfigService> mappingConfigService;

  public MappingConfigController(Optional<ElasticsearchMappingConfigService> mappingConfigService) {
    this.mappingConfigService = mappingConfigService;
  }

  @GetMapping("/{country}/{language}/mapping-config")
  public List<MapConfig> load(
    @PathVariable String country,
    @PathVariable String language
  ) {
    return mappingConfigService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .loadLatest(country, language);
  }

  @PutMapping("/{country}/{language}/mapping-config")
  public void save(
    @PathVariable String country,
    @PathVariable String language,
    @RequestBody List<MapConfig> configs
  ) {
    mappingConfigService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .save(country, language, configs);
  }
}
