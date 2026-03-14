package de.kittelberger.illusion.model;

import lombok.Data;

@Data
public class SkuMetaData extends MetaData{

  public SkuMetaData(String name, Long id) {
    super(name, id);
  }

  private String sku;
}
