package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.model.Information;
import de.kittelberger.illusion.service.InformationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InformationController {

  private final InformationService informationService;

  public InformationController(InformationService informationService) {
    this.informationService = informationService;
  }

  @GetMapping("/{country}/{language}/info/")
  public Information index(
    @PathVariable String country,
    @PathVariable String language
  ) {
    return informationService.loadInformation(country, language);
  }
}
