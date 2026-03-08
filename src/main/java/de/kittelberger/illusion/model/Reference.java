package de.kittelberger.illusion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Reference {
  private Long id;
  private String ukey;
  private AttrClassRef attrClasses;
}
