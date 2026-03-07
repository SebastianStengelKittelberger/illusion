package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.util.ClUtil;
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
      String value = ClUtil.getCleanedValue(ctx.product().getName(), ctx.locale());
      result.put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
      return;
    }

    ctx.skuAttributes().getFirstAttribute(ukey).ifPresent(attrval -> {
      String value = ctx.localizedTextExtractor().apply(attrval);
      result.put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
    });
  }
}
