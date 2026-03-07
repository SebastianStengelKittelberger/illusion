package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.SkuAttributes;
import de.kittelberger.webexport602w.solr.api.dto.ProductDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;

import java.util.Locale;
import java.util.function.Function;

public record MappingContext(
  SkuAttributes skuAttributes,
  ProductDTO product,
  Locale locale,
  Function<Attrval, String> localizedTextExtractor
) {}
