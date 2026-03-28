package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.data.ElasticsearchMappingConfigService;
import de.kittelberger.illusion.model.MapConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MappingConfigControllerTest {

  @Mock
  private ElasticsearchMappingConfigService mappingConfigService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(new MappingConfigController(Optional.of(mappingConfigService)))
      .build();
  }

  // ---------------------------------------------------------------------------
  // GET /{country}/{language}/mapping-config
  // ---------------------------------------------------------------------------

  @Test
  void load_returns200WithConfigList() throws Exception {
    MapConfig config = new MapConfig();
    config.setUkey("TITLE");
    config.setMappingType("TEXT");
    config.setTargetField("name");
    config.setTargetFieldType("STRING");

    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of(config));

    mockMvc.perform(get("/de/de/mapping-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].ukey").value("TITLE"))
      .andExpect(jsonPath("$[0].mappingType").value("TEXT"))
      .andExpect(jsonPath("$[0].targetField").value("name"));
  }

  @Test
  void load_emptyConfig_returnsEmptyArray() throws Exception {
    when(mappingConfigService.loadLatest("de", "de")).thenReturn(List.of());

    mockMvc.perform(get("/de/de/mapping-config"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$").isEmpty());
  }

  // ---------------------------------------------------------------------------
  // PUT /{country}/{language}/mapping-config
  // ---------------------------------------------------------------------------

  @Test
  void save_returns200AndDelegatesToService() throws Exception {
    String body = """
      [{"ukey":"TITLE","mappingType":"TEXT","targetField":"name","targetFieldType":"STRING"}]
      """;

    mockMvc.perform(put("/de/de/mapping-config")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andExpect(status().isOk());

    verify(mappingConfigService).save(eq("de"), eq("de"), any());
  }

  @Test
  void save_emptyList_returns200() throws Exception {
    mockMvc.perform(put("/de/de/mapping-config")
        .contentType(MediaType.APPLICATION_JSON)
        .content("[]"))
      .andExpect(status().isOk());

    verify(mappingConfigService).save(eq("de"), eq("de"), eq(List.of()));
  }
}
