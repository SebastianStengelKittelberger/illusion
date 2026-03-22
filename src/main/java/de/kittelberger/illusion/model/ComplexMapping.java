package de.kittelberger.illusion.model;

import lombok.Data;

import java.util.List;

@Data
public class ComplexMapping {

  private List<String> referencedAttrClasses;
  private List<String> producttypeAttrClassesToGroup;

}
