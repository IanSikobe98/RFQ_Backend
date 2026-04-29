package com.kingdom_bank.RFQBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RfqBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RfqBackendApplication.class, args);
	}

}
