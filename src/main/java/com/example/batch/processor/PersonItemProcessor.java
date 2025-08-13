package com.example.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.example.batch.model.Person;

/**
 * ItemProcessor implementation that normalizes Person data.
 * Currently it trims and converts the person's name to upper case.
 */
public class PersonItemProcessor implements ItemProcessor<Person, Person> {

    @Override
    public Person process(Person item) {
        if (item.getName() != null) {
            item.setName(item.getName().trim().toUpperCase());
        }
        return item;
    }
}

