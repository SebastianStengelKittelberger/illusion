package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.service.JavaParserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(100)
public class JavaCodeMappingHandler implements MappingHandler {

  private static final ExpressionParser SPEL = new SpelExpressionParser();

  private final JavaParserService javaParserService;

  public JavaCodeMappingHandler(JavaParserService javaParserService) {
    this.javaParserService = javaParserService;
  }

  @Override
  public boolean supports(MapConfig config) {
    return StringUtils.isNotBlank(config.getJavaCode());
  }

  @Override
  public void apply(
    final MapConfig config,
    final MappingContext ctx,
    Map<String, Map<String, Pair<String, Object>>> result) {
    for(String key : ctx.skuAttributes().getSkuAttributesList().keySet()) {
      String codeLine = javaParserService.replaceAttributeCalls(config.getJavaCode(), ctx.skuAttributes(), key);
      try {
        Object value = SPEL.parseExpression(codeLine).getValue();
        if (result.containsKey(key)) {
          result.get(key).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
        } else {
          result.put(key, new HashMap<>(Map.of(config.getTargetField(), Pair.of(config.getTargetFieldType(), value))));
        }
      } catch (Exception ignored) {
      }
    }
  }
}
