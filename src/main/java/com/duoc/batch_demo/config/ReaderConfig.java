package com.duoc.batch_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

import com.duoc.batch_demo.model.Person;

@Configuration
public class ReaderConfig {

    @Bean
    public FlatFileItemReader<Person> itemReader() {
        // Lector para leer un archivo CSV
        FlatFileItemReader<Person> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("data.csv"));
        reader.setLinesToSkip(1); // saltar la cabecera

        // Tokenizer: define los nombres (en el mismo orden del CSV)
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("id", "name", "age");
        // lineTokenizer.setDelimiter(","); // opcional; por defecto es coma

        // Mapeo de campos a la clase Person
        BeanWrapperFieldSetMapper<Person> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Person.class);

        // LineMapper que une tokenizer + mapper
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        reader.setLineMapper(lineMapper);
        return reader;
    }
}
