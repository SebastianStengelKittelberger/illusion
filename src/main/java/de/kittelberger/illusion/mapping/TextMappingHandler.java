package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.MapConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(1)
public class TextMappingHandler implements MappingHandler {

  @Override
  public boolean supports(MapConfig config) {
    return "TEXT".equals(config.getMappingType());
  }

  @Override
  public void apply(MapConfig config, MappingContext ctx, Map<String, Pair<String, Object>> result) {
    if (!"STRING".equals(config.getTargetFieldType())) return;

    String ukey = config.getUkey();
    boolean isFallback = Boolean.TRUE.equals(config.getIsFallback());

    if (isFallback && result.containsKey(config.getTargetField())) return;

    if ("$NAME$".equals(ukey)) {
      String value = ctx.product().productMetaData() != null
        ? ctx.product().productMetaData().getName()
        : null;
      result.put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
      return;
    }

    ctx.skuAttributes().getFirstAttribute(ukey).ifPresent(attribute -> {
      String value = extractText(attribute);
      result.put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
    });
  }

  private static String extractText(Attribute attribute) {
    if (attribute.getReferences() == null) return null;
    Map<String, Object> values = attribute.getReferences().right();
    Object cltext = values.get("CLTEXT");
    if (cltext != null) return cltext.toString();
    Object text = values.get("TEXT");
    return text != null ? text.toString() : null;
  }
}
