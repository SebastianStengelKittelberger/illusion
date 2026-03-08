package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.mapping.MappingContext;
import de.kittelberger.illusion.mapping.MappingHandler;
import de.kittelberger.illusion.model.DTOType;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.SkuAttributes;
import de.kittelberger.illusion.model.TargetType;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class IndexingService {

  private final LoadDataService loadDataService;
  private final List<MappingHandler> mappingHandlers;

  @Value("${xml.product-limit:-1}")
  private int productLimit;

  @Value("${xml.product-ids:}")
  private List<Long> productIds = new java.util.ArrayList<>();

  public IndexingService(
    final LoadDataService loadDataService,
    final List<MappingHandler> mappingHandlers
  ) {
    this.loadDataService = loadDataService;
    this.mappingHandlers = mappingHandlers;
  }

  public Map<String, Pair<String, Object>> indexProduct(
    List<MapConfig> mapConfigs,
    final String country,
    final String language
  ) {
    Map<String, Pair<String, Object>> resultMap = new ConcurrentHashMap<>();
    List<MapConfig> productConfigs = mapConfigs.stream()
      .filter(config -> config.getTarget().equals(TargetType.PRODUCT)).toList();
    List<Product> products = loadDataService.getProducts(country, language);
    if (productIds != null && !productIds.isEmpty()) {
      products = products.stream()
        .filter(p -> p.productMetaData() != null && productIds.contains(p.productMetaData().getId()))
        .toList();
    }
    if (productLimit > 0) {
      products = products.stream().limit(productLimit).toList();
    }
    List<MapConfig> skuMapConfigs = productConfigs.stream()
      .filter(m -> m.getDtoType().equals(DTOType.SKU)).toList();
    List<MapConfig> productDtoConfigs = productConfigs.stream()
      .filter(m -> m.getDtoType().equals(DTOType.PRODUCT)).toList();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Void>> futures = products.stream()
        .map(product -> executor.<Void>submit(() -> {
          MappingContext ctx = new MappingContext(
            new SkuAttributes(
              product.skuAttributes() != null ? product.skuAttributes() : List.of(),
              product.productAttributes() != null ? product.productAttributes() : List.of()
            ),
            product,
            Locale.of(language, country.toUpperCase())
          );

          skuMapConfigs.forEach(config ->
            mappingHandlers.stream()
              .filter(h -> h.supports(config))
              .forEach(h -> h.apply(config, ctx, resultMap))
          );
          productDtoConfigs.forEach(config ->
            mappingHandlers.stream()
              .filter(h -> h.supports(config))
              .forEach(h -> h.apply(config, ctx, resultMap))
          );
          return null;
        }))
        .toList();

      futures.forEach(f -> {
        try { f.get(); } catch (Exception e) {
          throw new RuntimeException("Fehler bei der Produkt-Verarbeitung", e);
        }
      });
    }

    return resultMap;
  }

}

