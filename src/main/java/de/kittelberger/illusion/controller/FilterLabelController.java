package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.data.ElasticsearchLabelService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class FilterLabelController {

  private final Optional<ElasticsearchLabelService> labelService;

  public FilterLabelController(Optional<ElasticsearchLabelService> labelService) {
    this.labelService = labelService;
  }

  @GetMapping("/{country}/{language}/filter-labels")
  public Map<String, String> load(
    @PathVariable String country,
    @PathVariable String language
  ) {
    return labelService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .loadFilterLabels(country, language);
  }

  @PutMapping("/{country}/{language}/filter-labels")
  public void save(
    @PathVariable String country,
    @PathVariable String language,
    @RequestBody Map<String, String> labels
  ) {
    labelService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .saveFilterLabels(country, language, labels);
  }
}
