package de.kittelberger.illusion;

import de.kittelberger.illusion.data.LoadDataService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class IllusionApplicationTests {

  @MockitoBean
  LoadDataService loadDataService;

  @Test
  void contextLoads() {
  }

}
