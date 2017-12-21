package com.dump.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Main class for Spring Boot application
 */
@SpringBootApplication
@EnableScheduling
public class ServiceApplication {

    /**
     * Application entry point
     * @param args  Command line arguments
     */
	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}
}
