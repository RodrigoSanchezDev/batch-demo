package com.duoc.batch_demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcTransactionManager;

import com.duoc.batch_demo.config.ProcessorConfig;
import com.duoc.batch_demo.config.ReaderConfig;
import com.duoc.batch_demo.config.WriterConfig;
import com.duoc.batch_demo.model.AnomaliaTransaccion;
import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.CuentaAnual;
import com.duoc.batch_demo.model.EstadoCuentaAnual;
import com.duoc.batch_demo.model.InteresCalculado;
import com.duoc.batch_demo.model.Transaccion;

@Configuration
@EnableBatchProcessing
@Import({DataSourceConfiguration.class, ReaderConfig.class, ProcessorConfig.class, WriterConfig.class})
public class BankBatchApplication {

    // ============================================
    // JOB 1: REPORTE DE TRANSACCIONES DIARIAS
    // ============================================
    @Bean
    public Step transaccionesStep(JobRepository jobRepository,
                                 JdbcTransactionManager transactionManager,
                                 ItemReader<Transaccion> transaccionReader,
                                 ItemProcessor<Transaccion, Transaccion> transaccionItemProcessor,
                                 ItemWriter<Transaccion> transaccionWriter) {
        return new StepBuilder("transaccionesStep", jobRepository)
                .<Transaccion, Transaccion>chunk(10, transactionManager)
                .reader(transaccionReader)
                .processor(transaccionItemProcessor)
                .writer(transaccionWriter)
                .build();
    }

    @Bean
    public Job reporteTransaccionesJob(JobRepository jobRepository, Step transaccionesStep) {
        return new JobBuilder("reporteTransaccionesJob", jobRepository)
                .start(transaccionesStep)
                .build();
    }

    // ============================================
    // JOB 2: CÁLCULO DE INTERESES MENSUALES
    // ============================================
    @Bean
    public Step interesesStep(JobRepository jobRepository,
                             JdbcTransactionManager transactionManager,
                             ItemReader<Cuenta> cuentaReader,
                             ItemProcessor<Cuenta, Cuenta> interesesItemProcessor,
                             ItemWriter<Cuenta> cuentaWriter) {
        return new StepBuilder("interesesStep", jobRepository)
                .<Cuenta, Cuenta>chunk(10, transactionManager)
                .reader(cuentaReader)
                .processor(interesesItemProcessor)
                .writer(cuentaWriter)
                .build();
    }

    @Bean
    public Job calculoInteresesJob(JobRepository jobRepository, Step interesesStep) {
        return new JobBuilder("calculoInteresesJob", jobRepository)
                .start(interesesStep)
                .build();
    }

    // ============================================
    // JOB 3: GENERACIÓN DE ESTADOS DE CUENTA ANUALES
    // ============================================
    @Bean
    public Step cuentasAnualesStep(JobRepository jobRepository,
                                  JdbcTransactionManager transactionManager,
                                  ItemReader<CuentaAnual> cuentaAnualReader,
                                  ItemProcessor<CuentaAnual, CuentaAnual> cuentaAnualItemProcessor,
                                  ItemWriter<CuentaAnual> cuentaAnualWriter) {
        return new StepBuilder("cuentasAnualesStep", jobRepository)
                .<CuentaAnual, CuentaAnual>chunk(10, transactionManager)
                .reader(cuentaAnualReader)
                .processor(cuentaAnualItemProcessor)
                .writer(cuentaAnualWriter)
                .build();
    }

    @Bean
    public Job estadosCuentaAnualesJob(JobRepository jobRepository, Step cuentasAnualesStep) {
        return new JobBuilder("estadosCuentaAnualesJob", jobRepository)
                .start(cuentasAnualesStep)
                .build();
    }

    // ============================================
    // JOB PRINCIPAL QUE EJECUTA TODOS LOS PROCESOS
    // ============================================
    @Bean
    public Job procesamientoBancarioCompletoJob(JobRepository jobRepository, 
                                               Step transaccionesStep,
                                               Step interesesStep,
                                               Step cuentasAnualesStep) {
        return new JobBuilder("procesamientoBancarioCompletoJob", jobRepository)
                .start(transaccionesStep)
                .next(interesesStep)
                .next(cuentasAnualesStep)
                .build();
    }

    // ============================================
    // STEPS ADICIONALES PARA GUARDAR DETALLES
    // ============================================
    
    @Bean
    public Step interesesDetalleStep(JobRepository jobRepository,
                                   JdbcTransactionManager transactionManager,
                                   ItemReader<Cuenta> cuentaReader,
                                   ItemProcessor<Cuenta, InteresCalculado> interesCalculadoItemProcessor,
                                   ItemWriter<InteresCalculado> interesCalculadoWriter) {
        return new StepBuilder("interesesDetalleStep", jobRepository)
                .<Cuenta, InteresCalculado>chunk(10, transactionManager)
                .reader(cuentaReader)
                .processor(interesCalculadoItemProcessor)
                .writer(interesCalculadoWriter)
                .build();
    }

    @Bean
    public Step anomaliasStep(JobRepository jobRepository,
                            JdbcTransactionManager transactionManager,
                            ItemReader<Transaccion> transaccionReader,
                            ItemProcessor<Transaccion, java.util.List<AnomaliaTransaccion>> anomaliaTransaccionItemProcessor,
                            ItemWriter<AnomaliaTransaccion> anomaliaTransaccionWriter) {
        return new StepBuilder("anomaliasStep", jobRepository)
                .<Transaccion, java.util.List<AnomaliaTransaccion>>chunk(10, transactionManager)
                .reader(transaccionReader)
                .processor(anomaliaTransaccionItemProcessor)
                .writer(items -> {
                    // Flatten la lista de listas y escribir cada anomalía
                    java.util.List<AnomaliaTransaccion> todasLasAnomalias = new java.util.ArrayList<>();
                    for (java.util.List<AnomaliaTransaccion> anomalias : items.getItems()) {
                        if (anomalias != null && !anomalias.isEmpty()) {
                            todasLasAnomalias.addAll(anomalias);
                        }
                    }
                    if (!todasLasAnomalias.isEmpty()) {
                        anomaliaTransaccionWriter.write(Chunk.of(todasLasAnomalias.toArray(new AnomaliaTransaccion[0])));
                    }
                })
                .build();
    }

    @Bean
    public Step estadosDetalleStep(JobRepository jobRepository,
                                 JdbcTransactionManager transactionManager,
                                 ItemReader<CuentaAnual> cuentaAnualReader,
                                 ItemProcessor<CuentaAnual, EstadoCuentaAnual> estadoCuentaAnualItemProcessor,
                                 ItemWriter<EstadoCuentaAnual> estadoCuentaAnualWriter) {
        return new StepBuilder("estadosDetalleStep", jobRepository)
                .<CuentaAnual, EstadoCuentaAnual>chunk(10, transactionManager)
                .reader(cuentaAnualReader)
                .processor(estadoCuentaAnualItemProcessor)
                .writer(estadoCuentaAnualWriter)
                .build();
    }

    // Método principal que ejecuta el sistema bancario batch completo
    public static void main(String[] args) throws Exception {
        System.out.println("\n🏦 ===== SISTEMA DE PROCESAMIENTO BANCARIO BATCH ===== 🏦");
        System.out.println("🚀 Iniciando procesamiento de datos bancarios legacy...\n");

        /*
        * Crea un contexto de aplicación basado en anotaciones, cargando la configuración
        * especificada en la clase BankBatchApplication.        
        */          
        ApplicationContext context = new AnnotationConfigApplicationContext(BankBatchApplication.class);

        /* 
        * Obtiene el bean de JobLauncher desde el contexto. El JobLauncher se encarga de ejecutar
        * trabajos batch configurados en Spring Batch.
        */
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);

        // ============================================
        // EJECUTAR LOS 3 JOBS PRINCIPALES
        // ============================================
        
        System.out.println("📊 === EJECUTANDO JOB 1: REPORTE DE TRANSACCIONES DIARIAS ===");
        Job reporteTransaccionesJob = context.getBean("reporteTransaccionesJob", Job.class);
        jobLauncher.run(reporteTransaccionesJob, new JobParameters());

        System.out.println("\n💰 === EJECUTANDO JOB 2: CÁLCULO DE INTERESES MENSUALES ===");
        Job calculoInteresesJob = context.getBean("calculoInteresesJob", Job.class);
        jobLauncher.run(calculoInteresesJob, new JobParameters());

        System.out.println("\n📋 === EJECUTANDO JOB 3: GENERACIÓN DE ESTADOS DE CUENTA ANUALES ===");
        Job estadosCuentaAnualesJob = context.getBean("estadosCuentaAnualesJob", Job.class);
        jobLauncher.run(estadosCuentaAnualesJob, new JobParameters());

        System.out.println("\n✅ ===== PROCESAMIENTO BANCARIO COMPLETADO EXITOSAMENTE ===== ✅");
        System.out.println("📈 Todos los datos han sido procesados y almacenados en la base de datos.");
        System.out.println("🔍 Revisa las tablas: transacciones, cuentas, cuentas_anuales para ver los resultados.\n");
    }
}
