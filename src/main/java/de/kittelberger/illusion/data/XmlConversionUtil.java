package de.kittelberger.illusion.data;

import de.kittelberger.webexport602w.solr.api.generated.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public final class XmlConversionUtil {

  private XmlConversionUtil() {}

  public static Date toDate(XMLGregorianCalendar cal) {
    return cal != null ? cal.toGregorianCalendar().getTime() : null;
  }

  public static Long toLong(BigInteger bi) {
    return bi != null ? bi.longValue() : null;
  }

  public static Character toChar(ActionInfo action) {
    return action != null ? action.value().charAt(0) : null;
  }

  @SuppressWarnings("rawtypes")
  public static Map toMap(ClngTextList2000 list) {
    if (list == null) return null;
    Map<String, String> map = new LinkedHashMap<>();
    for (ClngTextList2000.ClText2000 e : list.getClText2000()) {
      map.put(e.getCl(), e.getValue());
    }
    return map;
  }

  @SuppressWarnings("rawtypes")
  public static Map toMap(ClngTextList150 list) {
    if (list == null) return null;
    Map<String, String> map = new LinkedHashMap<>();
    for (ClngTextList150.ClText150 e : list.getClText150()) {
      map.put(e.getCl(), e.getValue());
    }
    return map;
  }

  @SuppressWarnings("rawtypes")
  public static Map toMap(ClngTextList50 list) {
    if (list == null) return null;
    Map<String, String> map = new LinkedHashMap<>();
    for (ClngTextList50.ClText50 e : list.getClText50()) {
      map.put(e.getCl(), e.getValue());
    }
    return map;
  }
}
