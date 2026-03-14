package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.MapConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(10)
public class TextMappingHandler implements MappingHandler {

  @Override
  public boolean supports(MapConfig config) {
    return "TEXT".equals(config.getMappingType());
  }

  @Override
  public void apply(
    final MapConfig config,
    final MappingContext ctx,
    Map<String, Map<String, Pair<String, Object>>> result) {
    if (!"STRING".equals(config.getTargetFieldType()))
      return;

    String ukey = config.getUkey();
    boolean isFallback = Boolean.TRUE.equals(config.getIsFallback());

    if (isFallback && result.containsKey(config.getTargetField()))
      return;
    for (String key : ctx.skuAttributes().getSkuAttributesList().keySet()) {
      if ("$NAME$".equals(ukey)) {
        String value = ctx.product().productMetaData() != null
          ? ctx.product().productMetaData().getName()
          : null;
        if (result.containsKey(key)) {
          result.get(key).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
        } else {
          result.put(key, Map.of(config.getTargetField(), Pair.of(config.getTargetFieldType(), value)));
        }
        continue;
      }

      ctx.skuAttributes().getFirstAttribute(key, ukey).ifPresent(attribute -> {
        String value = extractText(attribute);
        if (result.containsKey(key)) {
          result.get(key).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
        } else {
          result.put(key, new HashMap<>(Map.of(config.getTargetField(), Pair.of(config.getTargetFieldType(), value))));
        }
      });
    }
  }

  private static String extractText(Attribute attribute) {
    if (attribute.getReferences() == null) return null;
    Map<String, Object> values = attribute.getReferences();
    Object cltext = values.get("CLTEXT");
    if (cltext != null) return cltext.toString();
    Object text = values.get("TEXT");
    return text != null ? text.toString() : null;
  }
}
