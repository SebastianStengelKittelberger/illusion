package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.MapConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface CategoryMappingHandler {

  boolean supports(MapConfig config);

  void apply(MapConfig config, CategoryMappingContext ctx, Map<String, Map<String, Pair<String, Object>>> result);
}
