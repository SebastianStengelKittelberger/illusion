package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.MapConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Order(110)
public class ProductVariantMappingHandler implements MappingHandler{

  @Override
  public boolean supports(MapConfig config) {
    return "PRODUCT_VARIANTS".equals(config.getTargetField());
  }

  @Override
  public void apply(
    final MapConfig config,
    final MappingContext ctx,
    Map<String, Map<String, Pair<String, Object>>> result
  ) {
    Set<String> skus = ctx.skuAttributes().getSkuAttributesList().keySet();
    for (String key : skus) {
        if (result.containsKey(key)) {
          result.get(key).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), skus));
        } else {
          result.put(key, new HashMap<>(Map.of(config.getTargetField(), Pair.of(config.getTargetFieldType(), skus))));
        }
      }
  }
}
