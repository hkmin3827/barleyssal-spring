package com.hakyung.barleyssal_spring;

import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
public class BarleyssalSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarleyssalSpringApplication.class, args);
	}

}
