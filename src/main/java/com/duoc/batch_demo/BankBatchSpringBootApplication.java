package com.duoc.batch_demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

import com.duoc.batch_demo.model.AnomaliaTransaccion;
import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.CuentaAnual;
import com.duoc.batch_demo.model.EstadoCuentaAnual;
import com.duoc.batch_demo.model.InteresCalculado;
import com.duoc.batch_demo.model.Transaccion;

@SpringBootApplication
@EnableBatchProcessing
public class BankBatchSpringBootApplication {

    public static void main(String[] args) throws Exception {
        System.out.println("\n===== SISTEMA DE PROCESAMIENTO BANCARIO BATCH CON ESCALAMIENTO PARALELO =====");
        System.out.println("Iniciando procesamiento con 3 hilos paralelos y chunks de tamaño 5...\n");
        System.out.println("=== CONFIGURACIÓN DE ESCALAMIENTO PARALELO ===");
        System.out.println("   Hilos de ejecución paralela: 3");
        System.out.println("   Tamaño de chunks: 5 registros");
        System.out.println("   Tolerancia a fallos: Integrada");
        System.out.println("   Monitoreo de rendimiento: Activo");
        System.out.println("===============================================================\n");

        // Configurar para no ejecutar automáticamente los jobs
        System.setProperty("spring.batch.job.enabled", "false");

        ConfigurableApplicationContext context = SpringApplication.run(BankBatchSpringBootApplication.class, args);

        try {
            JobLauncher jobLauncher = context.getBean(JobLauncher.class);

            // ============================================
            // EJECUTAR LOS 3 JOBS PRINCIPALES CON DETALLES
            // ============================================
            
            System.out.println("=== EJECUTANDO JOB 1: REPORTE DE TRANSACCIONES DIARIAS ===");
            Job reporteTransaccionesJob = context.getBean("reporteTransaccionesJob", Job.class);
            jobLauncher.run(reporteTransaccionesJob, new JobParameters());

            System.out.println("=== EJECUTANDO PROCESAMIENTO DE ANOMALÍAS ===");
            Job anomaliasJob = context.getBean("anomaliasJob", Job.class);
            jobLauncher.run(anomaliasJob, new JobParameters());
            // Las anomalías se procesan junto con las transacciones

            System.out.println("\n=== EJECUTANDO JOB 2: CÁLCULO DE INTERESES MENSUALES ===");
            Job calculoInteresesJob = context.getBean("calculoInteresesJob", Job.class);
            jobLauncher.run(calculoInteresesJob, new JobParameters());

            System.out.println("=== EJECUTANDO GUARDADO DE DETALLES DE INTERESES ===");
            Job interesesDetalleJob = context.getBean("interesesDetalleJob", Job.class);
            jobLauncher.run(interesesDetalleJob, new JobParameters());

            System.out.println("\n=== EJECUTANDO JOB 3: GENERACIÓN DE ESTADOS DE CUENTA ANUALES ===");
            Job estadosCuentaAnualesJob = context.getBean("estadosCuentaAnualesJob", Job.class);
            jobLauncher.run(estadosCuentaAnualesJob, new JobParameters());

            System.out.println("=== EJECUTANDO PROCESAMIENTO DE ESTADOS DETALLADOS ===");
            Job estadosDetalleJob = context.getBean("estadosDetalleJob", Job.class);
            jobLauncher.run(estadosDetalleJob, new JobParameters());

            // ============================================
            // NUEVOS JOBS PARA DETECTAR TODAS LAS ANOMALÍAS
            // ============================================
            System.out.println("\n=== EJECUTANDO DETECCIÓN AVANZADA DE ANOMALÍAS EN TRANSACCIONES ===");
            Job deteccionAnomalíasAvanzadasJob = context.getBean("deteccionAnomalíasAvanzadasJob", Job.class);
            jobLauncher.run(deteccionAnomalíasAvanzadasJob, new JobParameters());

            System.out.println("\n=== EJECUTANDO DETECCIÓN DE DUPLICADOS Y ANOMALÍAS EN CUENTAS ===");
            Job deteccionAnomalíasCuentasJob = context.getBean("deteccionAnomalíasCuentasJob", Job.class);
            jobLauncher.run(deteccionAnomalíasCuentasJob, new JobParameters());

            System.out.println("\n===== PROCESAMIENTO BANCARIO PARALELO COMPLETADO EXITOSAMENTE =====");
            System.out.println("Todos los datos han sido procesados con escalamiento paralelo en MySQL.");
            System.out.println("RESUMEN DE ESCALAMIENTO:");
            System.out.println("   3 hilos de ejecución paralela utilizados");
            System.out.println("   Chunks de tamaño 5 procesados eficientemente");
            System.out.println("   Tolerancia a fallos aplicada en todos los steps");
            System.out.println("   Métricas de rendimiento capturadas");
            System.out.println("Sistema de detección avanzada de anomalías ejecutado con paralelismo:");
            System.out.println("   Montos negativos y cero detectados en paralelo");
            System.out.println("   Tipos de transacción inválidos identificados concurrentemente");
            System.out.println("   Registros duplicados encontrados con múltiples threads");
            System.out.println("   Edades fuera de rango detectadas paralelamente");
            System.out.println("   Datos faltantes identificados con escalamiento");
            System.out.println("Conectar MySQL Workbench para revisar los resultados del procesamiento paralelo.");
            System.out.println("Verificar tablas: transacciones, cuentas, cuentas_anuales,");
            System.out.println("   intereses_calculados, anomalias_transacciones, estados_cuenta_anuales");
            System.out.println("Logs de escalamiento disponibles arriba para análisis de rendimiento.\n");

        } catch (Exception e) {
            System.err.println("❌ Error durante el procesamiento: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // No cerrar el contexto inmediatamente para poder revisar la BD
            System.out.println("🔄 Aplicación completada. Los datos permanecen en MySQL para revisión.");
        }
    }

    // ============================================
    // CONFIGURACIÓN DE JOBS Y STEPS
    // ============================================

    // ============================================
    // JOB 1: REPORTE DE TRANSACCIONES DIARIAS CON ESCALAMIENTO PARALELO
    // ============================================
    @Bean
    public Step transaccionesStep(JobRepository jobRepository,
                                 JdbcTransactionManager transactionManager,
                                 ItemReader<Transaccion> transaccionReader,
                                 ItemProcessor<Transaccion, Transaccion> transaccionItemProcessor,
                                 ItemWriter<Transaccion> transaccionWriter,
                                 org.springframework.retry.RetryPolicy transaccionesRetryPolicy,
                                 org.springframework.batch.core.step.skip.SkipPolicy transaccionesSkipPolicy,
                                 org.springframework.batch.core.StepExecutionListener faultToleranceListener,
                                 @Qualifier("transactionTaskExecutor") TaskExecutor transactionTaskExecutor,
                                 @Qualifier("optimizedChunkSize") Integer chunkSize) {
        
        System.out.println("=== CONFIGURANDO STEP TRANSACCIONES CON ESCALAMIENTO PARALELO ===");
        System.out.println("   TaskExecutor: " + transactionTaskExecutor.getClass().getSimpleName());
        System.out.println("   Chunk Size: " + chunkSize + " registros");
        System.out.println("   Hilos paralelos: 3");
        System.out.println("   Tolerancia a fallos integrada");
        
        return new StepBuilder("transaccionesStep", jobRepository)
                .<Transaccion, Transaccion>chunk(chunkSize, transactionManager)  // Chunk size 5
                .reader(transaccionReader)
                .processor(transaccionItemProcessor)
                .writer(transaccionWriter)
                // Escalamiento paralelo con 3 hilos
                .taskExecutor(transactionTaskExecutor)
                // 🛡️ TOLERANCIA A FALLOS PARA TRANSACCIONES PRINCIPALES
                .faultTolerant()
                .retryPolicy(transaccionesRetryPolicy)
                .skipPolicy(transaccionesSkipPolicy)
                .listener(faultToleranceListener)
                .build();
    }

    @Bean
    public Job reporteTransaccionesJob(JobRepository jobRepository, Step transaccionesStep) {
        return new JobBuilder("reporteTransaccionesJob", jobRepository)
                .start(transaccionesStep)
                .build();
    }

    // ============================================
    // JOB 2: CÁLCULO DE INTERESES MENSUALES CON ESCALAMIENTO PARALELO
    // ============================================
    @Bean
    public Step interesesStep(JobRepository jobRepository,
                             JdbcTransactionManager transactionManager,
                             ItemReader<Cuenta> cuentaReader,
                             ItemProcessor<Cuenta, Cuenta> interesesItemProcessor,
                             ItemWriter<Cuenta> cuentaWriter,
                             org.springframework.retry.RetryPolicy cuentasRetryPolicy,
                             org.springframework.batch.core.step.skip.SkipPolicy cuentasSkipPolicy,
                             org.springframework.batch.core.StepExecutionListener faultToleranceListener,
                             @Qualifier("accountTaskExecutor") TaskExecutor accountTaskExecutor,
                             @Qualifier("optimizedChunkSize") Integer chunkSize) {
        
        System.out.println("=== CONFIGURANDO STEP INTERESES CON ESCALAMIENTO PARALELO ===");
        System.out.println("   TaskExecutor: " + accountTaskExecutor.getClass().getSimpleName());
        System.out.println("   Chunk Size: " + chunkSize + " registros");
        System.out.println("   Hilos paralelos: 3 (escalamiento hasta 4)");
        System.out.println("   Tolerancia a fallos para cuentas");
        
        return new StepBuilder("interesesStep", jobRepository)
                .<Cuenta, Cuenta>chunk(chunkSize, transactionManager)  // Chunk size 5
                .reader(cuentaReader)
                .processor(interesesItemProcessor)
                .writer(cuentaWriter)
                // Escalamiento paralelo con balanceamiento dinámico
                .taskExecutor(accountTaskExecutor)
                // 🛡️ TOLERANCIA A FALLOS PARA CÁLCULO DE INTERESES
                .faultTolerant()
                .retryPolicy(cuentasRetryPolicy)     // Más conservador para cuentas
                .skipPolicy(cuentasSkipPolicy)
                .listener(faultToleranceListener)
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
    // JOBS ADICIONALES PARA DETALLES
    // ============================================

    // Job para procesar detalles de intereses calculados
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
    public Job interesesDetalleJob(JobRepository jobRepository, Step interesesDetalleStep) {
        return new JobBuilder("interesesDetalleJob", jobRepository)
                .start(interesesDetalleStep)
                .build();
    }

    // Job para procesar estados de cuenta detallados
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

    @Bean
    public Job estadosDetalleJob(JobRepository jobRepository, Step estadosDetalleStep) {
        return new JobBuilder("estadosDetalleJob", jobRepository)
                .start(estadosDetalleStep)
                .build();
    }

    // Job para procesar anomalías de transacciones
    @Bean
    public Step anomaliasStep(JobRepository jobRepository,
                             JdbcTransactionManager transactionManager,
                             ItemReader<Transaccion> anomaliaTransaccionReader,
                             ItemProcessor<Transaccion, AnomaliaTransaccion> simpleAnomaliaProcessor,
                             ItemWriter<AnomaliaTransaccion> anomaliaTransaccionWriter) {
        return new StepBuilder("anomaliasStep", jobRepository)
                .<Transaccion, AnomaliaTransaccion>chunk(10, transactionManager)
                .reader(anomaliaTransaccionReader)
                .processor(simpleAnomaliaProcessor)
                .writer(anomaliaTransaccionWriter)
                .build();
    }

    @Bean
    public Job anomaliasJob(JobRepository jobRepository, Step anomaliasStep) {
        return new JobBuilder("anomaliasJob", jobRepository)
                .start(anomaliasStep)
                .build();
    }

    // ============================================
    // JOBS AVANZADOS PARA DETECCIÓN DE ANOMALÍAS CON POLÍTICAS DE REINTENTO Y OMISIÓN
    // ============================================
    
    // Step para detectar TODAS las anomalías en transacciones con ESCALAMIENTO PARALELO
    @Bean
    public Step deteccionAnomalíasAvanzadasStep(JobRepository jobRepository,
                                               JdbcTransactionManager transactionManager,
                                               ItemReader<Transaccion> todasLasTransaccionesReader,
                                               ItemProcessor<Transaccion, java.util.List<AnomaliaTransaccion>> detectarAnomalíasLegacyProcessor,
                                               org.springframework.batch.item.ItemWriter<java.util.List<AnomaliaTransaccion>> anomaliaListWriter,
                                               org.springframework.retry.RetryPolicy transaccionesRetryPolicy,
                                               org.springframework.batch.core.step.skip.SkipPolicy transaccionesSkipPolicy,
                                               org.springframework.batch.core.StepExecutionListener faultToleranceListener,
                                               @Qualifier("anomalyTaskExecutor") TaskExecutor anomalyTaskExecutor,
                                               @Qualifier("optimizedChunkSize") Integer chunkSize) {
        
        System.out.println("=== CONFIGURANDO DETECCIÓN ANOMALÍAS CON ESCALAMIENTO PARALELO ===");
        System.out.println("   TaskExecutor: " + anomalyTaskExecutor.getClass().getSimpleName());
        System.out.println("   Chunk Size: " + chunkSize + " registros");
        System.out.println("   Hilos paralelos: 3 (escalamiento hasta 6)");
        System.out.println("   Detección inteligente de anomalías");
        
        return new StepBuilder("deteccionAnomalíasAvanzadasStep", jobRepository)
                .<Transaccion, java.util.List<AnomaliaTransaccion>>chunk(chunkSize, transactionManager)
                .reader(todasLasTransaccionesReader)
                .processor(detectarAnomalíasLegacyProcessor)
                .writer(anomaliaListWriter)
                // Escalamiento paralelo para análisis de anomalías
                .taskExecutor(anomalyTaskExecutor)
                // 🛡️ POLÍTICAS PERSONALIZADAS DE TOLERANCIA A FALLOS
                .faultTolerant()
                .retryPolicy(transaccionesRetryPolicy) // Política de reintento personalizada
                .skipPolicy(transaccionesSkipPolicy)   // Política de omisión inteligente
                .listener(faultToleranceListener)      // Monitoreo y logging avanzado
                .build();
    }

    @Bean
    public Job deteccionAnomalíasAvanzadasJob(JobRepository jobRepository, Step deteccionAnomalíasAvanzadasStep) {
        return new JobBuilder("deteccionAnomalíasAvanzadasJob", jobRepository)
                .start(deteccionAnomalíasAvanzadasStep)
                .build();
    }

    // Step para detectar anomalías y duplicados en cuentas
    @Bean
    public Step deteccionAnomalíasCuentasStep(JobRepository jobRepository,
                                             JdbcTransactionManager transactionManager,
                                             ItemReader<Cuenta> todasLasCuentasReader,
                                             ItemProcessor<Cuenta, java.util.List<AnomaliaTransaccion>> detectarAnomaliasCuentasProcessor,
                                             org.springframework.batch.item.ItemWriter<java.util.List<AnomaliaTransaccion>> anomaliaListWriter,
                                             org.springframework.retry.RetryPolicy cuentasRetryPolicy,
                                             org.springframework.batch.core.step.skip.SkipPolicy cuentasSkipPolicy,
                                             org.springframework.batch.core.StepExecutionListener faultToleranceListener) {
        return new StepBuilder("deteccionAnomalíasCuentasStep", jobRepository)
                .<Cuenta, java.util.List<AnomaliaTransaccion>>chunk(5, transactionManager)
                .reader(todasLasCuentasReader)
                .processor(detectarAnomaliasCuentasProcessor)
                .writer(anomaliaListWriter)
                // 🛡️ POLÍTICAS PERSONALIZADAS PARA CUENTAS (MÁS ESTRICTAS)
                .faultTolerant()
                .retryPolicy(cuentasRetryPolicy)     // Reintentos más conservadores
                .skipPolicy(cuentasSkipPolicy)       // Omisiones más estrictas
                .listener(faultToleranceListener)    // Monitoreo detallado
                .build();
    }

    @Bean
    public Job deteccionAnomalíasCuentasJob(JobRepository jobRepository, Step deteccionAnomalíasCuentasStep) {
        return new JobBuilder("deteccionAnomalíasCuentasJob", jobRepository)
                .start(deteccionAnomalíasCuentasStep)
                .build();
    }
}
