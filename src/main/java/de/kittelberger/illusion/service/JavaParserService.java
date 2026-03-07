package de.kittelberger.illusion.service;

import de.kittelberger.illusion.model.SkuAttributes;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.illusion.data.XmlConversionUtil;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaParserService {

  private static final Pattern SKU_ATTR_PATTERN =
    Pattern.compile("\\$skuAttr\\((\\w+)\\)\\$\\.(\\w+\\(\\))");

  private static final Map<String, Function<Attrval, Object>> METHOD_HANDLERS = Map.of(
    "getTextVal()", Attrval::getTextval,
    "getNumVal()",  attrval -> attrval.getNumval() != null ? attrval.getNumval().getValue() : null,
    "getDateVal()", attrval -> XmlConversionUtil.toDate(attrval.getDateval())
  );

  public String replaceAttributeCalls(
    final String code,
    final SkuAttributes skuAttributes
  ) {
    Matcher matcher = SKU_ATTR_PATTERN.matcher(code);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String ukey       = matcher.group(1);
      String methodCall = matcher.group(2);

      Function<Attrval, Object> handler = METHOD_HANDLERS.get(methodCall);
      if (handler == null) {
        matcher.appendReplacement(sb, "null");
        continue;
      }

      String value = skuAttributes.getFirstAttribute(ukey)
        .map(handler)
        .map(Object::toString)
        .orElse("");

      matcher.appendReplacement(sb, Matcher.quoteReplacement("'" + value.replace("'", "\\'") + "'"));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
