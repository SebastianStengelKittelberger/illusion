package de.kittelberger.illusion.model;

import lombok.Data;

import java.util.List;

@Data
public class InformationRequestData {

  private List<MapConfig> mapConfigs;
  private String sku;

}
