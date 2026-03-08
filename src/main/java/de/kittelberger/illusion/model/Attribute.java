package de.kittelberger.illusion.model;

import lombok.Data;

import java.util.Map;

@Data
public class Attribute {

  private String ukey;
  private Map<String, Long> referenceIds;
  /**
   * key: type (TEXT, BOOLEAN, CLTEXT), value: actual value
   */
  private Map<String, Object> references;

}
