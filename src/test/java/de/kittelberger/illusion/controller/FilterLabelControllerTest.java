package de.kittelberger.illusion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kittelberger.illusion.data.ElasticsearchLabelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FilterLabelControllerTest {

  @Mock
  private ElasticsearchLabelService labelService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(new FilterLabelController(Optional.of(labelService)))
      .build();
  }

  // ---------------------------------------------------------------------------
  // GET /{country}/{language}/filter-labels
  // ---------------------------------------------------------------------------

  @Test
  void load_returns200WithLabels() throws Exception {
    when(labelService.loadFilterLabels("de", "de"))
      .thenReturn(Map.of("COLOR", "Farbe", "VOLTAGE", "Spannung"));

    mockMvc.perform(get("/de/de/filter-labels"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.COLOR").value("Farbe"))
      .andExpect(jsonPath("$.VOLTAGE").value("Spannung"));
  }

  @Test
  void load_emptyLabels_returnsEmptyObject() throws Exception {
    when(labelService.loadFilterLabels("de", "de")).thenReturn(Map.of());

    mockMvc.perform(get("/de/de/filter-labels"))
      .andExpect(status().isOk())
      .andExpect(content().json("{}"));
  }

  @Test
  void load_usesCountryAndLanguageFromPath() throws Exception {
    when(labelService.loadFilterLabels("gb", "en")).thenReturn(Map.of("COLOR", "Colour"));

    mockMvc.perform(get("/gb/en/filter-labels"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.COLOR").value("Colour"));

    verify(labelService).loadFilterLabels("gb", "en");
  }

  // ---------------------------------------------------------------------------
  // PUT /{country}/{language}/filter-labels
  // ---------------------------------------------------------------------------

  @Test
  void save_returns200AndDelegatesToService() throws Exception {
    Map<String, String> labels = Map.of("COLOR", "Farbe");

    mockMvc.perform(put("/de/de/filter-labels")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(labels)))
      .andExpect(status().isOk());

    verify(labelService).saveFilterLabels(eq("de"), eq("de"), eq(labels));
  }

  @Test
  void save_emptyMap_returns200() throws Exception {
    mockMvc.perform(put("/de/de/filter-labels")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
      .andExpect(status().isOk());

    verify(labelService).saveFilterLabels(eq("de"), eq("de"), eq(Map.of()));
  }

  // ---------------------------------------------------------------------------
  // No Elasticsearch
  // ---------------------------------------------------------------------------

  @Test
  void load_withoutElasticsearch_throwsIllegalState() {
    MockMvc noEsMvc = MockMvcBuilders
      .standaloneSetup(new FilterLabelController(Optional.empty()))
      .build();

    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
        noEsMvc.perform(get("/de/de/filter-labels"))
          .andReturn()
    ).hasCauseInstanceOf(IllegalStateException.class);
  }
}
