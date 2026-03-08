package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.MediaObject;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.SkuAttributes;

import java.util.Locale;
import java.util.Map;

public record MappingContext(
  SkuAttributes skuAttributes,
  Product product,
  Locale locale,
  Map<Long, MediaObject> mediaObjectMap,
  String domain
) {}
