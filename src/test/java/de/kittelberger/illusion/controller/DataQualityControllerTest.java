package de.kittelberger.illusion.controller;

import de.kittelberger.illusion.model.DataQuality;
import de.kittelberger.illusion.service.DataQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DataQualityControllerTest {

  @Mock
  private DataQualityService dataQualityService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new DataQualityController(dataQualityService)).build();
  }

  // ---------------------------------------------------------------------------
  // GET /{country}/{language}/dataQuality/{ukey}/
  // ---------------------------------------------------------------------------

  @Test
  void getDataQuality_returns200WithDataQualityJson() throws Exception {
    DataQuality dq = DataQuality.builder()
      .ukey("TITLE")
      .percentage("75% haben den UKEY. Das sind 3 von 4 Skus.")
      .skusWithoutUkey(List.of("SKU-004"))
      .build();
    when(dataQualityService.getDataQuality("TITLE", "de", "de")).thenReturn(dq);

    mockMvc.perform(get("/de/de/dataQuality/TITLE/"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ukey").value("TITLE"))
      .andExpect(jsonPath("$.percentage").value("75% haben den UKEY. Das sind 3 von 4 Skus."))
      .andExpect(jsonPath("$.skusWithoutUkey[0]").value("SKU-004"));
  }

  @Test
  void getDataQuality_allSkusPresent_returnsEmptySkusWithoutUkeyList() throws Exception {
    DataQuality dq = DataQuality.builder()
      .ukey("TITLE")
      .percentage("100% haben den UKEY. Das sind 2 von 2 Skus.")
      .skusWithoutUkey(List.of())
      .build();
    when(dataQualityService.getDataQuality("TITLE", "de", "de")).thenReturn(dq);

    mockMvc.perform(get("/de/de/dataQuality/TITLE/"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.skusWithoutUkey").isArray())
      .andExpect(jsonPath("$.skusWithoutUkey").isEmpty());
  }

  // ---------------------------------------------------------------------------
  // GET /{country}/{language}/dataQuality/{ukey}/values
  // ---------------------------------------------------------------------------

  @Test
  void getSkuValues_returns200WithSkuValueArray() throws Exception {
    when(dataQualityService.getSkuValues("TITLE", "de", "de"))
      .thenReturn(List.of(
        Map.of("sku", "SKU-001", "value", "Bohrmaschine"),
        Map.of("sku", "SKU-002", "value", "Schlagbohrmaschine")
      ));

    mockMvc.perform(get("/de/de/dataQuality/TITLE/values"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].sku").value("SKU-001"))
      .andExpect(jsonPath("$[0].value").value("Bohrmaschine"))
      .andExpect(jsonPath("$[1].sku").value("SKU-002"))
      .andExpect(jsonPath("$[1].value").value("Schlagbohrmaschine"));
  }

  @Test
  void getSkuValues_emptyResult_returnsEmptyArray() throws Exception {
    when(dataQualityService.getSkuValues("UNKNOWN", "de", "de")).thenReturn(List.of());

    mockMvc.perform(get("/de/de/dataQuality/UNKNOWN/values"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$").isEmpty());
  }
}
