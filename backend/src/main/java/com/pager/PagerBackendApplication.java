package com.pager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PagerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagerBackendApplication.class, args);
	}

}
