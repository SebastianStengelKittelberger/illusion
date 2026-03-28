package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.DataQuality;
import de.kittelberger.illusion.model.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class DataQualityService {

  private final LoadDataService loadDataService;

  public DataQualityService(LoadDataService loadDataService) {
    this.loadDataService = loadDataService;
  }

  public DataQuality getDataQuality(final String ukey, final String country, final String language) {
    List<Product> products = loadDataService.getProducts(country, language);
    return getDataQuality(ukey, products);
  }

  public DataQuality getDataQuality(String ukey, List<Product> products) {
    int numberOfSkus = products.stream().mapToInt(product -> product.skuMetaData().size()).sum();

    List<String> skuWithoutUkey = products
      .stream()
      .map(product -> product.skuAttributes()
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue().stream().noneMatch(attr -> attr.getUkey().equals(ukey)))
        .map(Map.Entry::getKey)
        .toList())
      .flatMap(Collection::stream)
      .toList();
    return DataQuality.builder()
      .percentage(((numberOfSkus - skuWithoutUkey.size()) * 100 / numberOfSkus) + "% haben den UKEY. Das sind " + (numberOfSkus - skuWithoutUkey.size()) + " von " + numberOfSkus + " Skus.")
      .skusWithoutUkey(skuWithoutUkey)
      .ukey(ukey)
      .build();
  }

  public List<Map<String, String>> getSkuValues(String ukey, String country, String language) {
    List<Product> products = loadDataService.getProducts(country, language);
    List<Map<String, String>> result = new ArrayList<>();
    for (Product product : products) {
      for (Map.Entry<String, List<Attribute>> entry : product.skuAttributes().entrySet()) {
        String sku = entry.getKey();
        entry.getValue().stream()
          .filter(attr -> attr.getUkey().equals(ukey))
          .findFirst()
          .ifPresent(attr -> {
            String val = "";
            if (attr.getReferences() != null && !attr.getReferences().isEmpty()) {
              val = attr.getReferences().values().iterator().next().toString();
            }
            result.add(Map.of("sku", sku, "value", val));
          });
      }
    }
    return result;
  }

}
