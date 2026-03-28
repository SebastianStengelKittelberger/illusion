package de.kittelberger.illusion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Builder
@AllArgsConstructor
@Data
public class Information {

  private List<Attribute> skuAttributes;
  private List<Attribute> productAttributes;
  private Set<String> skuUkeys;
  private Set<String> productUkeys;

  private Set<String> mappedSkuUkeys;
  private Set<String> unmappedSkuUkeys;
  private Set<String> mappedProductUkeys;
  private Set<String> unmappedProductUkeys;

  private List<DataQuality> dataQualitySkus;
}
