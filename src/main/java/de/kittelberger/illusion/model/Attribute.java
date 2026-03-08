package de.kittelberger.illusion.model;

import lombok.Data;

import java.util.Map;

@Data
public class Attribute {

  private String ukey;
  private Map<String, Long> referenceIds;
  /**
   * left: UKEY, right: value map with keys TEXT, BOOLEAN, CLTEXT
   */
  private AttributeRef references;

}
