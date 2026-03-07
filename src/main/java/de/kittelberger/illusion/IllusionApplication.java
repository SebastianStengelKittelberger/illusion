package de.kittelberger.illusion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IllusionApplication {

	public static void main(String[] args) {
		SpringApplication.run(IllusionApplication.class, args);
	}

}
