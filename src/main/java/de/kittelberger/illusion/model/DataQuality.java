package de.kittelberger.illusion.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataQuality {

  private String ukey;
  private String percentage;
  private List<String> SkusWithoutUkey;

}
