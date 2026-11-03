package com.tfi.motorsport.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MotorsportAplication {

	public static void main(String[] args) {
		SpringApplication.run(MotorsportAplication.class, args);

		System.out.println("MotorsportAplication started");
	}

}
