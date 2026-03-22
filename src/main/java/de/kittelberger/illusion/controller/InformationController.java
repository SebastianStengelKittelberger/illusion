package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.model.Information;
import de.kittelberger.illusion.model.InformationRequestData;
import de.kittelberger.illusion.service.InformationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// TODO: MapConfigs sollten nicht als Payload kommen, sondern von zentraler Stelle abgerufen werden. Aus Zeitgründen erst mal so
@RestController
public class InformationController {

  private final InformationService informationService;

  public InformationController(InformationService informationService) {
    this.informationService = informationService;
  }

  @PostMapping("/{country}/{language}/info/")
  public Information index(
    @PathVariable String country,
    @PathVariable String language,
    @RequestBody InformationRequestData data
  ) {



    return informationService.loadInformation(country, language, data);
  }
}
