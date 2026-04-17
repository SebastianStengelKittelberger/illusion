package de.kittelberger.illusion.service;

import de.kittelberger.illusion.mapping.MappingContext;
import de.kittelberger.illusion.model.FilterConfig;
import de.kittelberger.illusion.model.FilterType;
import de.kittelberger.illusion.model.MapConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contributes a {@code _filters} map to each indexed SKU document.
 *
 * <p>For each {@link MapConfig} that has an enabled {@link FilterConfig}, one entry is added
 * to {@code _filters}:
 * <ul>
 *   <li>{@link FilterType#STANDARD}: copies the value already written to {@code targetField}.</li>
 *   <li>{@link FilterType#PREDICATE}: evaluates a SpEL expression (with
 *       {@code $skuAttr(UKEY)$.getText()} token substitution) and writes the result.</li>
 * </ul>
 */
@Slf4j
@Service
public class FilterIndexContributor {

  static final String FILTERS_FIELD = "filters";

  private static final ExpressionParser SPEL = new SpelExpressionParser();

  private final JavaParserService javaParserService;

  public FilterIndexContributor(JavaParserService javaParserService) {
    this.javaParserService = javaParserService;
  }

  /**
   * Enriches every SKU entry in {@code results} with a {@code _filters} map.
   *
   * @param mapConfigs all configs for the current indexing run (already filtered to PRODUCT target)
   * @param contexts   map of skuKey → MappingContext, used for predicate evaluation
   * @param results    mutable result map (skuKey → fieldName → (type, value))
   */
  public void contribute(
    List<MapConfig> mapConfigs,
    Map<String, MappingContext> contexts,
    Map<String, Map<String, Pair<String, Object>>> results
  ) {
    List<MapConfig> filterConfigs = mapConfigs.stream()
      .filter(c -> c.getFilterConfig() != null && c.getFilterConfig().isEnabled())
      .toList();

    if (filterConfigs.isEmpty()) {
      return;
    }

    results.forEach((skuKey, fields) -> {
      Map<String, Object> filters = new HashMap<>();

      for (MapConfig config : filterConfigs) {
        FilterConfig fc = config.getFilterConfig();

        if (fc.getFilterType() == FilterType.PREDICATE) {
          evaluatePredicate(config, skuKey, contexts, filters);
        } else {
          Pair<String, Object> pair = fields.get(config.getTargetField());
          if (pair != null && pair.getRight() != null) {
            filters.put(config.getUkey(), pair.getRight());
          }
        }
      }

      if (!filters.isEmpty()) {
        fields.put(FILTERS_FIELD, Pair.of("OBJECT", filters));
      }
    });
  }

  private void evaluatePredicate(
    MapConfig config,
    String skuKey,
    Map<String, MappingContext> contexts,
    Map<String, Object> filters
  ) {
    MappingContext ctx = contexts.get(skuKey);
    if (ctx == null || config.getFilterConfig().getPredicate() == null) return;

    String expression = javaParserService.replaceAttributeCalls(
      config.getFilterConfig().getPredicate(),
      ctx.skuAttributes(),
      skuKey
    );

    try {
      Object result = SPEL.parseExpression(expression).getValue();
      if (result != null) {
        filters.put(config.getUkey(), result);
      }
    } catch (Exception e) {
      log.debug("Filter predicate evaluation failed for ukey '{}', sku '{}': {}",
        config.getUkey(), skuKey, e.getMessage());
    }
  }
}
