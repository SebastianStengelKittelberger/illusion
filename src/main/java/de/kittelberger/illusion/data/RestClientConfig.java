package de.kittelberger.illusion.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient boschAdapterRestClient(@Value("${bosch.adapter.url}") String baseUrl) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(30));
    factory.setReadTimeout(Duration.ofMinutes(30));
    return RestClient.builder()
      .baseUrl(baseUrl)
      .requestFactory(factory)
      .build();
  }
}
