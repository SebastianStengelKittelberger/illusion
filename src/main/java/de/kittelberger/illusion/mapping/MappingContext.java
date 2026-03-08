package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.SkuAttributes;

import java.util.Locale;

public record MappingContext(
  SkuAttributes skuAttributes,
  Product product,
  Locale locale
) {}
