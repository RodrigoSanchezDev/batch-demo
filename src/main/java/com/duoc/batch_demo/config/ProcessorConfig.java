package com.duoc.batch_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.item.ItemProcessor;

import com.duoc.batch_demo.model.Person;
import com.duoc.batch_demo.processor.PersonItemProcessor;

@Configuration
public class ProcessorConfig {

    @Bean
    public ItemProcessor<Person, Person> itemProcessor() {
        return new PersonItemProcessor();
    }
}

