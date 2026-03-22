package de.kittelberger.illusion.util;

import de.kittelberger.illusion.model.Attribute;

import java.util.Map;

public class AttributeUtil {


  public static String extractText(Attribute attribute) {
    if (attribute.getReferences() == null) return null;
    Map<String, Object> values = attribute.getReferences();
    Object cltext = values.get("CLTEXT");
    if (cltext != null) return cltext.toString();
    Object text = values.get("TEXT");
    return text != null ? text.toString() : null;
  }
}
