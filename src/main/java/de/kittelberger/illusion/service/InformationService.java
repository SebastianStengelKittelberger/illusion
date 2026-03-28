package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.ElasticsearchMappingConfigService;
import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.model.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InformationService {

  private final LoadDataService loadDataService;
  private final DataQualityService dataQualityService;
  private final Optional<ElasticsearchMappingConfigService> mappingConfigService;

  public InformationService(
    final LoadDataService loadDataService,
    final DataQualityService dataQualityService,
    final Optional<ElasticsearchMappingConfigService> mappingConfigService
  ) {
    this.loadDataService = loadDataService;
    this.dataQualityService = dataQualityService;
    this.mappingConfigService = mappingConfigService;
  }

  @Cacheable("information")
  public Information loadInformation(
    final String country,
    final String language
  ) {
    List<Product> products = loadDataService.getProducts(country, language);

    List<Attribute> productAttributes = products.stream().flatMap(p -> p.productAttributes().stream()).toList();
    List<Attribute> skuAttributes = products.stream().flatMap(p -> p.skuAttributes().values().stream()).flatMap(List::stream).toList();

    Map<String, Long> productUkeyFrequency = products.stream()
      .flatMap(p -> p.productAttributes().stream().map(Attribute::getUkey).distinct())
      .collect(Collectors.groupingBy(ukey -> ukey, Collectors.counting()));
    Map<String, Long> skuUkeyFrequency = products.stream()
      .flatMap(p -> p.skuAttributes().values().stream().flatMap(List::stream).map(Attribute::getUkey).distinct())
      .collect(Collectors.groupingBy(ukey -> ukey, Collectors.counting()));

    Set<String> productUkeys = productUkeyFrequency.keySet();
    Set<String> skuUkeys = skuUkeyFrequency.keySet();

    // Split into mapped / unmapped based on current MappingConfig
    Set<String> configuredUkeys = mappingConfigService
      .map(svc -> svc.loadLatest(country, language).stream()
        .map(MapConfig::getUkey)
        .collect(Collectors.toSet()))
      .orElseGet(Set::of);

    Set<String> mappedSkuUkeys     = skuUkeys.stream().filter(configuredUkeys::contains).collect(Collectors.toSet());
    Set<String> unmappedSkuUkeys   = skuUkeys.stream().filter(u -> !configuredUkeys.contains(u)).collect(Collectors.toSet());
    Set<String> mappedProductUkeys   = productUkeys.stream().filter(configuredUkeys::contains).collect(Collectors.toSet());
    Set<String> unmappedProductUkeys = productUkeys.stream().filter(u -> !configuredUkeys.contains(u)).collect(Collectors.toSet());

    Set<String> seenProductUkeys = new HashSet<>();
    List<Attribute> uniqueProductAttributes = productAttributes.stream()
      .filter(attr -> seenProductUkeys.add(attr.getUkey()))
      .sorted(Comparator.comparingLong((Attribute attr) -> productUkeyFrequency.getOrDefault(attr.getUkey(), 0L)).reversed())
      .limit(10)
      .toList();

    Set<String> seenSkuUkeys = new HashSet<>();
    List<Attribute> uniqueSkuAttributes = skuAttributes.stream()
      .filter(attr -> seenSkuUkeys.add(attr.getUkey()))
      .sorted(Comparator.comparingLong((Attribute attr) -> skuUkeyFrequency.getOrDefault(attr.getUkey(), 0L)).reversed())
      .limit(10)
      .toList();

    List<DataQuality> dataQualitiesSku = skuUkeys.stream()
      .map(ukey -> dataQualityService.getDataQuality(ukey, products))
      .limit(10)
      .toList();

    return Information.builder()
      .dataQualitySkus(dataQualitiesSku)
      .productAttributes(uniqueProductAttributes)
      .skuAttributes(uniqueSkuAttributes)
      .productUkeys(productUkeys)
      .skuUkeys(skuUkeys)
      .mappedSkuUkeys(mappedSkuUkeys)
      .unmappedSkuUkeys(unmappedSkuUkeys)
      .mappedProductUkeys(mappedProductUkeys)
      .unmappedProductUkeys(unmappedProductUkeys)
      .build();
  }
}

