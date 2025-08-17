package com.duoc.batch_demo;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
public class DataSourceConfiguration {

	// DataSource se configurará automáticamente desde application.properties
	// No necesitamos override manual aquí para MySQL

	@Bean
	public JdbcTransactionManager transactionManager(DataSource dataSource) {
		return new JdbcTransactionManager(dataSource);
	}

}