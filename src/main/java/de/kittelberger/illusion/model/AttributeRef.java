package de.kittelberger.illusion.model;

import java.util.Map;

/**
 * Holds the resolved reference data for an {@link Attribute}.
 * {@code left} is the attribute UKEY; {@code right} is the value map
 * with keys such as {@code TEXT}, {@code BOOLEAN}, {@code CLTEXT}.
 */
public record AttributeRef(String left, Map<String, Object> right) {}
