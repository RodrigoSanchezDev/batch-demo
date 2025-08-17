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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
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
        System.out.println("\n🏦 ===== SISTEMA DE PROCESAMIENTO BANCARIO BATCH (SPRING BOOT) ===== 🏦");
        System.out.println("🚀 Iniciando procesamiento de datos bancarios legacy con MySQL...\n");

        // Configurar para no ejecutar automáticamente los jobs
        System.setProperty("spring.batch.job.enabled", "false");

        ConfigurableApplicationContext context = SpringApplication.run(BankBatchSpringBootApplication.class, args);

        try {
            JobLauncher jobLauncher = context.getBean(JobLauncher.class);

            // ============================================
            // EJECUTAR LOS 3 JOBS PRINCIPALES CON DETALLES
            // ============================================
            
            System.out.println("📊 === EJECUTANDO JOB 1: REPORTE DE TRANSACCIONES DIARIAS ===");
            Job reporteTransaccionesJob = context.getBean("reporteTransaccionesJob", Job.class);
            jobLauncher.run(reporteTransaccionesJob, new JobParameters());

            System.out.println("🚨 === EJECUTANDO PROCESAMIENTO DE ANOMALÍAS ===");
            Job anomaliasJob = context.getBean("anomaliasJob", Job.class);
            jobLauncher.run(anomaliasJob, new JobParameters());
            // Las anomalías se procesan junto con las transacciones

            System.out.println("\n💰 === EJECUTANDO JOB 2: CÁLCULO DE INTERESES MENSUALES ===");
            Job calculoInteresesJob = context.getBean("calculoInteresesJob", Job.class);
            jobLauncher.run(calculoInteresesJob, new JobParameters());

            System.out.println("💾 === EJECUTANDO GUARDADO DE DETALLES DE INTERESES ===");
            Job interesesDetalleJob = context.getBean("interesesDetalleJob", Job.class);
            jobLauncher.run(interesesDetalleJob, new JobParameters());

            System.out.println("\n📋 === EJECUTANDO JOB 3: GENERACIÓN DE ESTADOS DE CUENTA ANUALES ===");
            Job estadosCuentaAnualesJob = context.getBean("estadosCuentaAnualesJob", Job.class);
            jobLauncher.run(estadosCuentaAnualesJob, new JobParameters());

            System.out.println("📈 === EJECUTANDO PROCESAMIENTO DE ESTADOS DETALLADOS ===");
            Job estadosDetalleJob = context.getBean("estadosDetalleJob", Job.class);
            jobLauncher.run(estadosDetalleJob, new JobParameters());

            // ============================================
            // NUEVOS JOBS PARA DETECTAR TODAS LAS ANOMALÍAS
            // ============================================
            System.out.println("\n🔍 === EJECUTANDO DETECCIÓN AVANZADA DE ANOMALÍAS EN TRANSACCIONES ===");
            Job deteccionAnomalíasAvanzadasJob = context.getBean("deteccionAnomalíasAvanzadasJob", Job.class);
            jobLauncher.run(deteccionAnomalíasAvanzadasJob, new JobParameters());

            System.out.println("\n🔍 === EJECUTANDO DETECCIÓN DE DUPLICADOS Y ANOMALÍAS EN CUENTAS ===");
            Job deteccionAnomalíasCuentasJob = context.getBean("deteccionAnomalíasCuentasJob", Job.class);
            jobLauncher.run(deteccionAnomalíasCuentasJob, new JobParameters());

            System.out.println("\n✅ ===== PROCESAMIENTO BANCARIO COMPLETADO EXITOSAMENTE ===== ✅");
            System.out.println("📈 Todos los datos han sido procesados y almacenados en MySQL.");
            System.out.println("🔍 Sistema de detección avanzada de anomalías ejecutado:");
            System.out.println("   ✓ Montos negativos y cero detectados");
            System.out.println("   ✓ Tipos de transacción inválidos identificados");
            System.out.println("   ✓ Registros duplicados encontrados");
            System.out.println("   ✓ Edades fuera de rango detectadas");
            System.out.println("   ✓ Datos faltantes identificados");
            System.out.println("🔍 Ahora puedes conectar MySQL Workbench para ver los resultados.");
            System.out.println("💡 Verificando tablas: transacciones, cuentas, cuentas_anuales,");
            System.out.println("   intereses_calculados, anomalias_transacciones, estados_cuenta_anuales\n");

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
    
    // Step para detectar TODAS las anomalías en transacciones
    @Bean
    public Step deteccionAnomalíasAvanzadasStep(JobRepository jobRepository,
                                               JdbcTransactionManager transactionManager,
                                               ItemReader<Transaccion> todasLasTransaccionesReader,
                                               ItemProcessor<Transaccion, java.util.List<AnomaliaTransaccion>> detectarAnomalíasLegacyProcessor,
                                               org.springframework.batch.item.ItemWriter<java.util.List<AnomaliaTransaccion>> anomaliaListWriter) {
        return new StepBuilder("deteccionAnomalíasAvanzadasStep", jobRepository)
                .<Transaccion, java.util.List<AnomaliaTransaccion>>chunk(5, transactionManager)
                .reader(todasLasTransaccionesReader)
                .processor(detectarAnomalíasLegacyProcessor)
                .writer(anomaliaListWriter)
                // POLÍTICAS DE REINTENTO Y OMISIÓN
                .faultTolerant()
                .retry(RuntimeException.class)
                .retryLimit(3) // Reintentar hasta 3 veces
                .skip(Exception.class)
                .skipLimit(5) // Saltar hasta 5 registros problemáticos
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
                                             org.springframework.batch.item.ItemWriter<java.util.List<AnomaliaTransaccion>> anomaliaListWriter) {
        return new StepBuilder("deteccionAnomalíasCuentasStep", jobRepository)
                .<Cuenta, java.util.List<AnomaliaTransaccion>>chunk(5, transactionManager)
                .reader(todasLasCuentasReader)
                .processor(detectarAnomaliasCuentasProcessor)
                .writer(anomaliaListWriter)
                // POLÍTICAS DE REINTENTO Y OMISIÓN PARA CUENTAS
                .faultTolerant()
                .retry(RuntimeException.class)
                .retryLimit(2) // Menos reintentos para cuentas
                .skip(Exception.class)
                .skipLimit(3) // Saltar hasta 3 cuentas problemáticas
                .build();
    }

    @Bean
    public Job deteccionAnomalíasCuentasJob(JobRepository jobRepository, Step deteccionAnomalíasCuentasStep) {
        return new JobBuilder("deteccionAnomalíasCuentasJob", jobRepository)
                .start(deteccionAnomalíasCuentasStep)
                .build();
    }
}
