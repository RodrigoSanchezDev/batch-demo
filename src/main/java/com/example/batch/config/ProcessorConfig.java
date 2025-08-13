package com.example.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.item.ItemProcessor;

import com.example.batch.model.Person;
import com.example.batch.processor.PersonItemProcessor;

@Configuration
public class ProcessorConfig {

    @Bean
    public ItemProcessor<Person, Person> itemProcessor() {
        return new PersonItemProcessor();
    }
}

