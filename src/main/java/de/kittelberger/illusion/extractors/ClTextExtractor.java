package de.kittelberger.illusion.extractors;

import de.kittelberger.illusion.util.ClUtil;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;

import java.util.Locale;
import java.util.function.Function;

public class ClTextExtractor {

  private ClTextExtractor() {}
  public static final Function<Locale, Function<Attrval, String>> extractText = locale -> (attrval -> ClUtil.getAttrCltextValByLocale(attrval, locale));
}
