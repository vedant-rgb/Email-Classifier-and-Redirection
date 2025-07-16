package com.vedant.email_fetcher_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmailFetcherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailFetcherServiceApplication.class, args);
	}

}
