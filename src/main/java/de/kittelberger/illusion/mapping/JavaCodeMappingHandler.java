package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.service.JavaParserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(2)
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
  public void apply(MapConfig config, MappingContext ctx, Map<String, Pair<String, Object>> result) {
    String codeLine = javaParserService.replaceAttributeCalls(config.getJavaCode(), ctx.skuAttributes());
    try {
      Object value = SPEL.parseExpression(codeLine).getValue();
      result.put(config.getTargetField(), Pair.of(config.getTargetFieldType(), value));
    } catch (Exception ignored) {
    }
  }
}
