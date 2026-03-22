package de.kittelberger.illusion.model;

import lombok.Data;


@Data
public class MapConfig {

  private ComplexMapping complexMapping;
  private DTOType dtoType;
  private String ukey;
  private String mappingType;
  private String targetField;
  private String targetFieldType;
  private String javaCode;
  private Boolean isFallback;
  private TargetType target;
}
