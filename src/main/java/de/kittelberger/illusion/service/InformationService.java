package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InformationService {

  private final IndexingService indexingService;
  private final LoadDataService loadDataService;
  private final DataQualityService dataQualityService;

  public InformationService(
    final IndexingService indexingService,
    final LoadDataService loadDataService,
    final DataQualityService dataQualityService
  ) {
    this.indexingService = indexingService;
    this.loadDataService = loadDataService;
    this.dataQualityService = dataQualityService;
  }

  public Information loadInformation(
    final String country,
    final String language,
    final InformationRequestData data
  ) {
//    TODO: Das sollte auch nicht über den Service laufen, sondern von zentraler Stelle wie zB ElasticSearch geladen werden. Das ist verschwendete Performance. Aber leicht umzusetzen, also mein Freund gerade
    Map<String, Map<String, Pair<String, Object>>> mappedData = indexingService.indexProduct(data.getMapConfigs(), country, language);
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
      .build();
  }
}
