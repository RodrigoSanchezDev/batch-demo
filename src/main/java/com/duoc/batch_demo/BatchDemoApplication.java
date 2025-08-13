package com.duoc.batch_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class BatchDemoApplication {

@Bean
	public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
		return new StepBuilder("step", jobRepository).tasklet((contribution, chunkContext) -> {
			System.out.println("Hello world!");
			return RepeatStatus.FINISHED;
		}, transactionManager).build();
	}

	@Bean
	public Job job(JobRepository jobRepository, Step step) {
		return new JobBuilder("job", jobRepository).start(step).build();
	}


    // Metodo main de tu aplicacion
	public static void main(String[] args) throws Exception {
		/*
        * Crea un contexto de aplicación basado en anotaciones, cargando la configuración
        * especificada en la clase HelloWorldJobConfiguration.        
        */          
        ApplicationContext context = new AnnotationConfigApplicationContext(BatchDemoApplication.class);

        /* 
        * Obtiene el bean de JobLauncher desde el contexto. El JobLauncher se encarga de ejecutar
        * trabajos batch configurados en Spring Batch.
        */
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);

        /* 
        * Obtiene el bean del Job (trabajo batch) desde el contexto. El Job representa el proceso
        * batch que se va a ejecutar.
        */
        Job job = context.getBean(Job.class);

        /*
        * Ejecuta el trabajo batch utilizando el JobLauncher, pasando el Job y los parámetros
        * necesarios para su ejecución (en este caso, un conjunto vacío de parámetros).
        */
        jobLauncher.run(job, new JobParameters());
	}

}
