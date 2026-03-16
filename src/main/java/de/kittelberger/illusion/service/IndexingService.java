package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.mapping.MappingContext;
import de.kittelberger.illusion.mapping.MappingHandler;
import de.kittelberger.illusion.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndexingService {

  private final LoadDataService loadDataService;
  private final List<MappingHandler> mappingHandlers;

  @Value("${xml.product-limit:-1}")
  private int productLimit;

  @Value("${xml.product-ids:}")
  private List<Long> productIds = new java.util.ArrayList<>();

  @Value("${illusion.index.batch-size:100}")
  private int batchSize;

  public IndexingService(
    final LoadDataService loadDataService,
    final List<MappingHandler> mappingHandlers
  ) {
    this.loadDataService = loadDataService;
    this.mappingHandlers = mappingHandlers;
  }

  public Map<String, Map<String, Pair<String, Object>>> indexProduct(
    final List<MapConfig> mapConfigs,
    final String country,
    final String language
  ) {
    Map<String, Map<String, Pair<String, Object>>> results = new ConcurrentHashMap<>();
    List<MapConfig> productConfigs = mapConfigs.stream()
      .filter(config -> config.getTarget().equals(TargetType.PRODUCT)).toList();
    List<MapConfig> skuMapConfigs = productConfigs.stream()
      .filter(m -> m.getDtoType().equals(DTOType.SKU)).toList();
    List<MapConfig> productDtoConfigs = productConfigs.stream()
      .filter(m -> m.getDtoType().equals(DTOType.PRODUCT)).toList();
    Map<Long, Image> mediaObjects = loadDataService.getMediaObjects(country, language);
    String domain = loadDataService.getDomain(country, language);
    int effectiveBatchSize = Math.max(batchSize, 1);
    List<Product> batch = new ArrayList<>(effectiveBatchSize);
    AtomicInteger processedProducts = new AtomicInteger();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      loadDataService.streamProducts(country, language, product -> {
        if (!shouldProcess(product)) {
          return true;
        }

        batch.add(product);
        int count = processedProducts.incrementAndGet();
        if (batch.size() >= effectiveBatchSize || (productLimit > 0 && count >= productLimit)) {
          processBatch(
            batch,
            executor,
            skuMapConfigs,
            productDtoConfigs,
            country,
            language,
            mediaObjects,
            domain,
            results
          );
          batch.clear();
        }

        return productLimit <= 0 || count < productLimit;
      });

      if (!batch.isEmpty()) {
        processBatch(
          batch,
          executor,
          skuMapConfigs,
          productDtoConfigs,
          country,
          language,
          mediaObjects,
          domain,
          results
        );
        batch.clear();
      }
    }

    return results;
  }

  private boolean shouldProcess(Product product) {
    return productIds == null
      || productIds.isEmpty()
      || product.productMetaData() != null && productIds.contains(product.productMetaData().getId());
  }

  private void processBatch(
    List<Product> products,
    ExecutorService executor,
    List<MapConfig> skuMapConfigs,
    List<MapConfig> productDtoConfigs,
    String country,
    String language,
    Map<Long, Image> mediaObjects,
    String domain,
    Map<String, Map<String, Pair<String, Object>>> results
  ) {
    List<Future<Void>> futures = new ArrayList<>(products.size());
    for (Product product : List.copyOf(products)) {
      futures.add(executor.submit(() -> {
        processProduct(product, skuMapConfigs, productDtoConfigs, country, language, mediaObjects, domain, results);
        return null;
      }));
    }

    futures.forEach(f -> {
      try {
        f.get();
      } catch (Exception e) {
        throw new RuntimeException("Fehler bei der Produkt-Verarbeitung", e);
      }
    });
  }

  private void processProduct(
    Product product,
    List<MapConfig> skuMapConfigs,
    List<MapConfig> productDtoConfigs,
    String country,
    String language,
    Map<Long, Image> mediaObjects,
    String domain,
    Map<String, Map<String, Pair<String, Object>>> results
  ) {
    Map<String, Map<String, Pair<String, Object>>> productResult = new HashMap<>();
    MappingContext ctx = new MappingContext(
      new SkuAttributes(
        product.skuAttributes() != null ? product.skuAttributes() : Map.of(),
        product.productAttributes() != null ? product.productAttributes() : List.of()
      ),
      product,
      Locale.of(language, country.toUpperCase()),
      mediaObjects,
      domain
    );

    skuMapConfigs.forEach(config ->
      mappingHandlers.stream()
        .filter(h -> h.supports(config))
        .forEach(h -> h.apply(config, ctx, productResult))
    );
    productDtoConfigs.forEach(config ->
      mappingHandlers.stream()
        .filter(h -> h.supports(config))
        .forEach(h -> h.apply(config, ctx, productResult))
    );

    results.putAll(productResult);
  }
}
