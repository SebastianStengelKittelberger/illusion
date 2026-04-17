package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.data.ElasticsearchLabelService;
import de.kittelberger.illusion.data.ElasticsearchMappingConfigService;
import de.kittelberger.illusion.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FilterConfigControllerTest {

  @Mock
  private ElasticsearchMappingConfigService mappingConfigService;

  @Mock
  private ElasticsearchLabelService labelService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(new FilterConfigController(
        Optional.of(mappingConfigService),
        Optional.of(labelService)))
      .build();
  }

  // ---------------------------------------------------------------------------
  // GET /{country}/{language}/filter-config — happy path
  // ---------------------------------------------------------------------------

  @Test
  void getFilterConfig_returns200WithActiveFilters() throws Exception {
    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of(
      mapConfigWithFilter("COLOR", "color", filter(true, FilterType.STANDARD, null))
    ));
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of("COLOR", "Farbe"));

    mockMvc.perform(get("/de/de/filter-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].ukey").value("COLOR"))
      .andExpect(jsonPath("$[0].targetField").value("color"))
      .andExpect(jsonPath("$[0].label").value("Farbe"))
      .andExpect(jsonPath("$[0].filterConfig.filterType").value("STANDARD"));
  }

  @Test
  void getFilterConfig_usesUkeyAsFallbackLabel() throws Exception {
    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of(
      mapConfigWithFilter("VOLTAGE", "voltage", filter(true, FilterType.STANDARD, null))
    ));
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of()); // no label saved

    mockMvc.perform(get("/de/de/filter-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].label").value("VOLTAGE"));
  }

  @Test
  void getFilterConfig_excludesDisabledFilters() throws Exception {
    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of(
      mapConfigWithFilter("COLOR",   "color",   filter(true,  FilterType.STANDARD, null)),
      mapConfigWithFilter("VOLTAGE", "voltage", filter(false, FilterType.STANDARD, null))
    ));
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of());

    mockMvc.perform(get("/de/de/filter-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].ukey").value("COLOR"));
  }

  @Test
  void getFilterConfig_excludesConfigsWithoutFilterConfig() throws Exception {
    MapConfig plain = new MapConfig();
    plain.setUkey("TITLE");
    plain.setTargetField("name");
    // no filterConfig

    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of(plain));
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of());

    mockMvc.perform(get("/de/de/filter-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void getFilterConfig_sortsResultsByOrder() throws Exception {
    FilterConfig fc = filter(true, FilterType.STANDARD, null);
    fc.setOrder(2);
    FilterConfig fc2 = filter(true, FilterType.STANDARD, null);
    fc2.setOrder(1);

    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of(
      mapConfigWithFilter("VOLTAGE", "voltage", fc),   // order=2
      mapConfigWithFilter("COLOR",   "color",   fc2)   // order=1
    ));
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of());

    mockMvc.perform(get("/de/de/filter-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].ukey").value("COLOR"))
      .andExpect(jsonPath("$[1].ukey").value("VOLTAGE"));
  }

  @Test
  void getFilterConfig_includesPredicateType() throws Exception {
    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of(
      mapConfigWithFilter("IS_18V", "voltage",
        filter(true, FilterType.PREDICATE, "$skuAttr(VOLTAGE)$.getText() == '18V'"))
    ));
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of());

    mockMvc.perform(get("/de/de/filter-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].filterConfig.filterType").value("PREDICATE"))
      .andExpect(jsonPath("$[0].filterConfig.predicate").value("$skuAttr(VOLTAGE)$.getText() == '18V'"));
  }

  @Test
  void getFilterConfig_returnsEmptyWhenNoConfigs() throws Exception {
    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of());
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of());

    mockMvc.perform(get("/de/de/filter-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isEmpty());
  }

  // ---------------------------------------------------------------------------
  // No Elasticsearch
  // ---------------------------------------------------------------------------

  @Test
  void getFilterConfig_withoutElasticsearch_throwsIllegalState() {
    MockMvc noEsMvc = MockMvcBuilders
      .standaloneSetup(new FilterConfigController(Optional.empty(), Optional.of(labelService)))
      .build();

    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
        noEsMvc.perform(get("/de/de/filter-config"))
          .andReturn()
    ).hasCauseInstanceOf(IllegalStateException.class);
  }

  // ---------------------------------------------------------------------------
  // Test data helpers
  // ---------------------------------------------------------------------------

  private static FilterConfig filter(boolean enabled, FilterType type, String predicate) {
    FilterConfig fc = new FilterConfig();
    fc.setEnabled(enabled);
    fc.setFilterType(type);
    fc.setPredicate(predicate);
    return fc;
  }

  private static MapConfig mapConfigWithFilter(String ukey, String targetField, FilterConfig filterConfig) {
    MapConfig c = new MapConfig();
    c.setUkey(ukey);
    c.setTargetField(targetField);
    c.setTarget(TargetType.PRODUCT);
    c.setDtoType(DTOType.SKU);
    c.setMappingType("TEXT");
    c.setTargetFieldType("STRING");
    c.setFilterConfig(filterConfig);
    return c;
  }
}
