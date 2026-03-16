package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Image;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.SkuAttributes;

import java.util.Locale;
import java.util.Map;

public record MappingContext(
  SkuAttributes skuAttributes,
  Product product,
  Locale locale,
  Map<Long, Image> mediaObjectMap,
  String domain
) {}
