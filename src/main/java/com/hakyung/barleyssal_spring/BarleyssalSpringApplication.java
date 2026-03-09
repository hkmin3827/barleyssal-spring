package com.hakyung.barleyssal_spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class BarleyssalSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarleyssalSpringApplication.class, args);
	}

}
