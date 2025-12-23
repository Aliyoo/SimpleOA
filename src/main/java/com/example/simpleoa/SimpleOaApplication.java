package com.example.simpleoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SimpleOaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleOaApplication.class, args);
	}

}