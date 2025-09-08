package com.duoc.batch_demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

import com.duoc.batch_demo.config.BankDataPartitioner;
import com.duoc.batch_demo.config.PartitionConfig;
import com.duoc.batch_demo.listener.ScalingPerformanceListener;
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
        System.out.println("\n==============================================");
        System.out.println("🏦 SISTEMA BANCARIO BATCH + BFFs INICIANDO...");
        System.out.println("==============================================");
        System.out.println("📊 Modo: Servidor API (BFFs habilitados)");
        System.out.println("🌐 API Web BFF: http://localhost:8080/api/web/");
        System.out.println("� API Mobile BFF: http://localhost:8080/api/mobile/");
        System.out.println("� API ATM BFF: http://localhost:8080/api/atm/");
        System.out.println("� Documentación: http://localhost:8080/swagger-ui.html");
        System.out.println("==============================================");

        // Configurar para no ejecutar automáticamente los jobs
        System.setProperty("spring.batch.job.enabled", "false");

        SpringApplication.run(BankBatchSpringBootApplication.class, args);
        
        System.out.println("\n🚀 Servidor iniciado exitosamente!");
        System.out.println("📡 Escuchando en puerto 8080...");
        System.out.println("💡 Presiona Ctrl+C para detener el servidor");
        System.out.println("==============================================");
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

    // ============================================
    // JOBS CON PARTICIONES - NUEVA FUNCIONALIDAD
    // ============================================
    
    /**
     * Step worker (esclavo) para procesamiento particionado de transacciones.
     * Este step procesa una partición específica de transacciones.
     */
    @Bean
    public Step partitionedTransaccionWorkerStep(JobRepository jobRepository,
                                                  JdbcTransactionManager transactionManager,
                                                  ItemReader<Transaccion> partitionedTransaccionReader,
                                                  ItemProcessor<Transaccion, Transaccion> transaccionItemProcessor,
                                                  ItemWriter<Transaccion> transaccionWriter,
                                                  ScalingPerformanceListener scalingPerformanceListener,
                                                  @Qualifier("optimizedChunkSize") Integer chunkSize) {
        
        System.out.println("⚙️  Configurando Worker Step para Transacciones Particionadas");
        System.out.println("   • Chunk Size: " + chunkSize);
        System.out.println("   • Listener: PartitionPerformanceListener activo");
        
        return new StepBuilder("partitionedTransaccionWorkerStep", jobRepository)
                .<Transaccion, Transaccion>chunk(chunkSize, transactionManager)
                .reader(partitionedTransaccionReader) // Se resuelve dinámicamente por @StepScope
                .processor(transaccionItemProcessor)
                .writer(transaccionWriter)
                .listener(scalingPerformanceListener)
                .build();
    }
    
    /**
     * Step maestro para procesamiento particionado de transacciones.
     * Coordina múltiples particiones utilizando el partitioner y handler.
     */
    @Bean
    public Step partitionedTransaccionMasterStep(JobRepository jobRepository,
                                                  BankDataPartitioner bankDataPartitioner,
                                                  @Qualifier("partitionCoordinatorTaskExecutor") TaskExecutor coordinatorTaskExecutor,
                                                  Step partitionedTransaccionWorkerStep) {
        
        System.out.println("🎯 Configurando Master Step para Transacciones Particionadas:");
        System.out.println("   • Partitioner: BankDataPartitioner");
        System.out.println("   • Handler: PartitionCoordinator (4 particiones)");
        System.out.println("   • Worker: partitionedTransaccionWorkerStep (SIN multi-threading interno)");
        System.out.println("   • Estrategia: DISTRIBUCIÓN PURA");
        
        // Crear PartitionHandler inline con coordinator simple
        PartitionHandler partitionHandler = PartitionConfig.createTransactionPartitionHandler(coordinatorTaskExecutor, partitionedTransaccionWorkerStep);
        
        return new StepBuilder("partitionedTransaccionMasterStep", jobRepository)
                .partitioner("partitionedTransaccionWorkerStep", bankDataPartitioner)
                .partitionHandler(partitionHandler)
                .step(partitionedTransaccionWorkerStep)
                .build();
    }
    
    /**
     * Job para procesamiento de transacciones usando particiones.
     */
    @Bean
    public Job particionesTransaccionesJob(JobRepository jobRepository, 
                                           Step partitionedTransaccionMasterStep) {
        System.out.println("\n🚀 CREANDO JOB PARTICIONADO: TRANSACCIONES DISTRIBUIDAS");
        System.out.println("   📊 Estrategia: 4 particiones automáticas por rango de ID");
        System.out.println("   🔄 TaskExecutor: partitionCoordinatorTaskExecutor (1 hilo por partition)");
        System.out.println("   🎯 Procesamiento: SECUENCIAL dentro de cada partición");
        System.out.println("   📈 Escalabilidad: Distribución geográfica/temporal");
        
        return new JobBuilder("particionesTransaccionesJob", jobRepository)
                .start(partitionedTransaccionMasterStep)
                .build();
    }
    
    /**
     * Step worker para procesamiento particionado de cuentas.
     */
    @Bean
    public Step partitionedCuentaWorkerStep(JobRepository jobRepository,
                                           JdbcTransactionManager transactionManager,
                                           ItemReader<Cuenta> partitionedCuentaReader,
                                           ItemProcessor<Cuenta, Cuenta> interesesItemProcessor,
                                           ItemWriter<Cuenta> cuentaWriter,
                                           ScalingPerformanceListener scalingPerformanceListener,
                                           @Qualifier("optimizedChunkSize") Integer chunkSize) {
        
        System.out.println("⚙️  Configurando Worker Step para Cuentas Particionadas");
        
        return new StepBuilder("partitionedCuentaWorkerStep", jobRepository)
                .<Cuenta, Cuenta>chunk(chunkSize, transactionManager)
                .reader(partitionedCuentaReader) // Se resuelve dinámicamente por @StepScope
                .processor(interesesItemProcessor)
                .writer(cuentaWriter)
                .listener(scalingPerformanceListener)
                .build();
    }
    
    /**
     * Step maestro para procesamiento particionado de cuentas.
     */
    @Bean
    public Step partitionedCuentaMasterStep(JobRepository jobRepository,
                                           BankDataPartitioner bankDataPartitioner,
                                           @Qualifier("partitionCoordinatorTaskExecutor") TaskExecutor coordinatorTaskExecutor,
                                           Step partitionedCuentaWorkerStep) {
        
        System.out.println("🎯 Configurando Master Step para Cuentas Particionadas:");
        System.out.println("   • Handler: PartitionCoordinator (3 particiones)");
        System.out.println("   • Estrategia: DISTRIBUCIÓN PURA SIN multi-threading interno");
        
        // Crear PartitionHandler inline con coordinator simple
        PartitionHandler partitionHandler = PartitionConfig.createAccountPartitionHandler(coordinatorTaskExecutor, partitionedCuentaWorkerStep);
        
        return new StepBuilder("partitionedCuentaMasterStep", jobRepository)
                .partitioner("partitionedCuentaWorkerStep", bankDataPartitioner)
                .partitionHandler(partitionHandler)
                .step(partitionedCuentaWorkerStep)
                .build();
    }
    
    /**
     * Job para procesamiento de cuentas usando particiones.
     */
    @Bean
    public Job particionesCuentasJob(JobRepository jobRepository, 
                                    Step partitionedCuentaMasterStep) {
        System.out.println("🚀 CREANDO JOB PARTICIONADO: CUENTAS ANUALES DISTRIBUIDAS");
        System.out.println("   ⚙️ Sistema de Particiones Activo");
        System.out.println("   📊 Distribución: 3 Particiones (SIN multi-threading interno)");
        System.out.println("   🎯 Estrategia: DISTRIBUCIÓN GEOGRÁFICA O TEMPORAL");
        System.out.println("   💾 Escalable para millones de registros");
        System.out.println("   -------------------------------------------");
        
        return new JobBuilder("particionesCuentasJob", jobRepository)
                .start(partitionedCuentaMasterStep)
                .build();
    }
    
    /**
     * Step worker para detección particionada de anomalías.
     */
    @Bean
    public Step partitionedAnomaliaWorkerStep(JobRepository jobRepository,
                                             JdbcTransactionManager transactionManager,
                                             ItemReader<Transaccion> partitionedAnomaliaTransaccionReader,
                                             ItemProcessor<Transaccion, java.util.List<AnomaliaTransaccion>> detectarAnomalíasLegacyProcessor,
                                             org.springframework.batch.item.ItemWriter<java.util.List<AnomaliaTransaccion>> anomaliaListWriter,
                                             ScalingPerformanceListener scalingPerformanceListener) {
        
        System.out.println("🚨 Configurando Worker Step para Anomalías Particionadas");
        
        return new StepBuilder("partitionedAnomaliaWorkerStep", jobRepository)
                .<Transaccion, java.util.List<AnomaliaTransaccion>>chunk(3, transactionManager) // Chunks pequeños para anomalías
                .reader(partitionedAnomaliaTransaccionReader) // Se resuelve dinámicamente por @StepScope
                .processor(detectarAnomalíasLegacyProcessor)
                .writer(anomaliaListWriter)
                .listener(scalingPerformanceListener)
                .build();
    }
    
    /**
     * Step maestro para detección particionada de anomalías.
     */
    @Bean
    public Step partitionedAnomaliaMasterStep(JobRepository jobRepository,
                                             BankDataPartitioner bankDataPartitioner,
                                             @Qualifier("partitionCoordinatorTaskExecutor") TaskExecutor coordinatorTaskExecutor,
                                             Step partitionedAnomaliaWorkerStep) {
        
        System.out.println("🎯 Configurando Master Step para Anomalías Particionadas:");
        System.out.println("   • Handler: PartitionCoordinator (6 particiones)");
        System.out.println("   • Estrategia: DISTRIBUCIÓN POR TIPO DE ANOMALÍA");
        
        // Crear PartitionHandler inline con coordinator simple
        PartitionHandler partitionHandler = PartitionConfig.createAnomalyPartitionHandler(coordinatorTaskExecutor, partitionedAnomaliaWorkerStep);
        
        return new StepBuilder("partitionedAnomaliaMasterStep", jobRepository)
                .partitioner("partitionedAnomaliaWorkerStep", bankDataPartitioner)
                .partitionHandler(partitionHandler)
                .step(partitionedAnomaliaWorkerStep)
                .build();
    }
    
    /**
     * Job para detección de anomalías usando particiones.
     */
    @Bean
    public Job particionesAnomaliasJob(JobRepository jobRepository, 
                                      Step partitionedAnomaliaMasterStep) {
        System.out.println("🚀 CREANDO JOB PARTICIONADO: DETECCIÓN DE ANOMALÍAS AVANZADA");
        System.out.println("   🚨 Sistema de Detección Distribuido");
        System.out.println("   📊 Distribución: 6 Particiones (SIN multi-threading interno)");
        System.out.println("   🎯 Estrategia: DISTRIBUCIÓN POR TIPO DE ANOMALÍA");
        System.out.println("   🔍 Escalable para análisis masivos de transacciones");
        System.out.println("   -------------------------------------------");
        
        return new JobBuilder("particionesAnomaliasJob", jobRepository)
                .start(partitionedAnomaliaMasterStep)
                .build();
    }
}
