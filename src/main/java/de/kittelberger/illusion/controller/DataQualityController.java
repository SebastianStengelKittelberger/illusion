package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.model.DataQuality;
import de.kittelberger.illusion.service.DataQualityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DataQualityController {

  private final DataQualityService dataQualityService;

  public DataQualityController(DataQualityService dataQualityService) {
    this.dataQualityService = dataQualityService;
  }

  @GetMapping("/{country}/{language}/dataQuality/{ukey}/")
  public DataQuality getDataQuality(
    @PathVariable final String ukey,
    @PathVariable final String country,
    @PathVariable final String language
  ) {
    return dataQualityService.getDataQuality(ukey, country, language);
  }

  @GetMapping("/{country}/{language}/dataQuality/{ukey}/values")
  public List<Map<String, String>> getSkuValues(
    @PathVariable String ukey,
    @PathVariable String country,
    @PathVariable String language
  ) {
    return dataQualityService.getSkuValues(ukey, country, language);
  }

}
