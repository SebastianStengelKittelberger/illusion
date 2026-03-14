package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.MapConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface MappingHandler {

  boolean supports(MapConfig config);

  void apply(MapConfig config, MappingContext ctx, Map<String, Map<String, Pair<String, Object>>> result);
}
