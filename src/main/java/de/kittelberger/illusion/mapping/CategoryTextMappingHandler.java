package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.TargetType;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kittelberger.illusion.util.AttributeUtil.extractText;

/**
 * Handles TEXT mappings for categories (mappingType = "TEXT", targetFieldType = "STRING").
 * Reads the first matching {@link Attribute} from {@code category.attributes()} by ukey.
 */
@Component
@Order(10)
public class CategoryTextMappingHandler implements CategoryMappingHandler {

  @Override
  public boolean supports(MapConfig config) {
    return TargetType.CATEGORY.equals(config.getTarget())
      && "TEXT".equals(config.getMappingType())
      && "STRING".equals(config.getTargetFieldType());
  }

  @Override
  public void apply(
    final MapConfig config,
    final CategoryMappingContext ctx,
    final Map<String, Map<String, Pair<String, Object>>> result
  ) {
    String key = ctx.category().ukey();
    String ukey = config.getUkey();

    List<Attribute> attrs = ctx.category().attributes().get(ukey);
    String value = (attrs != null && !attrs.isEmpty()) ? extractText(attrs.get(0)) : null;

    if (result.containsKey(key)) {
      result.get(key).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
    } else {
      result.put(key, new HashMap<>(Map.of(config.getTargetField(), Pair.of(config.getTargetFieldType(), value))));
    }
  }
}
