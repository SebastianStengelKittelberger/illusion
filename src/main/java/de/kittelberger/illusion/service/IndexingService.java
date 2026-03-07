package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.extractors.ClTextExtractor;
import de.kittelberger.illusion.mapping.MappingContext;
import de.kittelberger.illusion.mapping.MappingHandler;
import de.kittelberger.illusion.model.DTOType;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.SkuAttributes;
import de.kittelberger.illusion.model.TargetType;
import de.kittelberger.webexport602w.solr.api.dto.AttrDTO;
import de.kittelberger.webexport602w.solr.api.dto.ProductDTO;
import de.kittelberger.webexport602w.solr.api.dto.SkuDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.Product;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    List<ProductDTO> products = loadDataService.getProductDTOs();
    if (productIds != null && !productIds.isEmpty()) {
      products = products.stream()
        .filter(x -> x.getId() != null && productIds.contains(x.getId()))
        .toList();
    }
    if (productLimit > 0) {
      products = products.stream().limit(productLimit).toList();
    }
    List<MapConfig> skuMapConfigs = productConfigs.stream()
      .filter(m -> m.getDtoType().equals(DTOType.SKU)).toList();
    Function<Attrval, String> localizedTextExtractor =
      ClTextExtractor.extractText.apply(Locale.of(language, country));

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Void>> futures = products.stream()
        .map(product -> executor.<Void>submit(() -> {
          List<String> skuIds = product.getSkus() != null
            ? product.getSkus().getSku().stream().map(Product.Skus.Sku::getSku).toList()
            : List.of();
          Map<String, List<SkuDTO>> skuDTOs = getSkuDTOsById(skuIds);
          List<Attrval> productAttributes = product.getAttrvals() != null
            ? product.getAttrvals().getAttrval()
            : List.of();
          List<Attrval> skuAttributeList = skuDTOs.values().stream()
            .flatMap(List::stream)
            .flatMap(sku -> sku.getAttrvals() != null
              ? sku.getAttrvals().getAttrval().stream() : Stream.empty())
            .toList();
          List<Long> skuAttrIds = skuAttributeList.stream()
            .map(Attrval::getAttrdId).filter(Objects::nonNull).map(BigInteger::longValue).toList();
          List<Long> productAttrIds = productAttributes.stream()
            .map(Attrval::getAttrdId).filter(Objects::nonNull).map(BigInteger::longValue).toList();
          Map<Long, AttrDTO> attrDTOs = loadDataService.getAttrDTOs().stream()
            .filter(attr -> skuAttrIds.contains(attr.getId()) || productAttrIds.contains(attr.getId()))
            .collect(Collectors.toMap(AttrDTO::getId, Function.identity()));

          MappingContext ctx = new MappingContext(
            new SkuAttributes(skuAttributeList, productAttributes, attrDTOs),
            product,
            Locale.of(language, country.toUpperCase()),
            localizedTextExtractor
          );

          skuMapConfigs.forEach(config ->
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

  public Map<String, List<SkuDTO>> getSkuDTOsById(List<String> skuIds) {
    Map<String, List<SkuDTO>> result = new HashMap<>();
    for (SkuDTO skuDTO : loadDataService.getSkuDTOs()) {
      if (skuIds.contains(String.valueOf(skuDTO.getSku()))) {
        result.computeIfAbsent(skuDTO.getSku(), k -> new ArrayList<>()).add(skuDTO);
      }
    }
    return result;
  }

}

