package de.kittelberger.illusion.model;

import java.util.List;

/**
 * Holds the resolved reference data for a {@link Reference}.
 * {@code left} is the reference UKEY; {@code right} is the list of attribute classes.
 */
public record AttrClassRef(String left, List<AttrClass> right) {}
