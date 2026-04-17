package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Category;

import java.util.Locale;

public record CategoryMappingContext(
  Category category,
  Locale locale
) {}
