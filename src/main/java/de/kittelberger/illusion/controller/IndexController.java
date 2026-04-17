package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.service.IndexingService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class IndexController {

  private final IndexingService indexingService;

  public IndexController(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @PostMapping("/{country}/{language}/index")
  public Map<String, Map<String,Pair<String, Object>>> index(
    @PathVariable String country,
    @PathVariable String language,
    @RequestBody List<MapConfig> mapConfigs
  ) {
    return indexingService.indexProduct(mapConfigs, country, language);
  }

  @PostMapping("/{country}/{language}/index/categories")
  public Map<String, Map<String, Pair<String, Object>>> indexCategories(
    @PathVariable String country,
    @PathVariable String language,
    @RequestBody List<MapConfig> mapConfigs
  ) {
    return indexingService.indexCategories(mapConfigs, country, language);
  }
}
