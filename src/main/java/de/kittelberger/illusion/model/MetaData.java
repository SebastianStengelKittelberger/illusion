package de.kittelberger.illusion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public abstract class MetaData {
  private String name;
  private Long id;
}
