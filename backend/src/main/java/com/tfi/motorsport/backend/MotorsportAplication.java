package com.tfi.motorsport.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MotorsportAplication {

	public static void main(String[] args) {
		SpringApplication.run(MotorsportAplication.class, args);

		System.out.println("MotorsportAplication started");
	}

}
