package com.hakyung.barleyssal_spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
@EnableScheduling
public class BarleyssalSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarleyssalSpringApplication.class, args);
	}

}
