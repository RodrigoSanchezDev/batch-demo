package com.duoc.batch_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración de políticas de escalamiento paralelo para Spring Batch.
 * 
 * Proporciona TaskExecutors optimizados para procesamiento concurrente
 * con configuraciones específicas por tipo de operación.
 * 
 * @author Rodrigo Sanchez
 * @version 1.1
 */
@Configuration
public class ScalingPolicyConfig {

    /**
     * Configuración del pool de threads principal para procesamiento bancario.
     * Optimizado para alta concurrencia con escalamiento automático.
     */
    @Bean(name = "bankBatchTaskExecutor")
    public TaskExecutor bankBatchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración de escalamiento paralelo
        executor.setCorePoolSize(3);           // 3 hilos base
        executor.setMaxPoolSize(5);            // Escalamiento hasta 5 hilos bajo carga
        executor.setQueueCapacity(50);         // Cola de tareas pendientes
        executor.setKeepAliveSeconds(60);      // Tiempo de vida threads adicionales
        
        // Configuración de threading policy
        executor.setThreadNamePrefix("BankBatch-Parallel-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        // Configuración de rendimiento
        executor.setAllowCoreThreadTimeOut(true);  // Optimización de recursos
        
        executor.initialize();
        
        System.out.println("TaskExecutor configurado:");
        System.out.println("   Core Pool Size: 3 hilos paralelos");
        System.out.println("   Max Pool Size: 5 hilos (escalamiento automático)");
        System.out.println("   Queue Capacity: 50 tareas en cola");
        System.out.println("   Chunk Size: 5 registros por chunk");
        
        return executor;
    }

    /**
     * TaskExecutor especializado para procesamiento de transacciones.
     * Configuración estable para garantizar consistencia de datos.
     */
    @Bean(name = "transactionTaskExecutor")
    public TaskExecutor transactionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración específica para transacciones
        executor.setCorePoolSize(3);           // 3 hilos paralelos fijos
        executor.setMaxPoolSize(3);            // Sin escalamiento adicional (estabilidad)
        executor.setQueueCapacity(30);         // Cola optimizada para transacciones
        executor.setKeepAliveSeconds(30);
        
        executor.setThreadNamePrefix("Transaction-Processor-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        System.out.println("Transaction TaskExecutor configurado:");
        System.out.println("   3 hilos paralelos estables");
        System.out.println("   Cola optimizada: 30 transacciones");
        System.out.println("   Política de consistencia aplicada");
        
        return executor;
    }

    /**
     * TaskExecutor para procesamiento de cuentas con escalamiento dinámico.
     * Balanceado para adaptarse a la carga de trabajo variable.
     */
    @Bean(name = "accountTaskExecutor")
    public TaskExecutor accountTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración balanceada para cuentas
        executor.setCorePoolSize(3);           // 3 hilos base
        executor.setMaxPoolSize(4);            // Escalamiento moderado
        executor.setQueueCapacity(40);         // Cola balanceada
        executor.setKeepAliveSeconds(45);
        
        executor.setThreadNamePrefix("Account-Processor-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(90);
        
        executor.initialize();
        
        System.out.println("Account TaskExecutor configurado:");
        System.out.println("   3 hilos base + 1 escalamiento dinámico");
        System.out.println("   Cola balanceada: 40 cuentas");
        System.out.println("   Escalamiento automático bajo carga");
        
        return executor;
    }

    /**
     * TaskExecutor especializado para detección de anomalías.
     * Configuración de alto rendimiento para análisis concurrente.
     */
    @Bean(name = "anomalyTaskExecutor")
    public TaskExecutor anomalyTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración de alto rendimiento
        executor.setCorePoolSize(3);           // 3 hilos paralelos
        executor.setMaxPoolSize(6);            // Escalamiento agresivo para análisis
        executor.setQueueCapacity(60);         // Cola extendida para picos de carga
        executor.setKeepAliveSeconds(90);      // Threads longevos para análisis complejos
        
        executor.setThreadNamePrefix("Anomaly-Detector-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(150);
        
        executor.initialize();
        
        System.out.println("Anomaly TaskExecutor configurado:");
        System.out.println("   3 hilos base + escalamiento hasta 6");
        System.out.println("   Cola extendida: 60 registros");
        System.out.println("   Alto rendimiento para detección");
        
        return executor;
    }

    /**
     * TaskExecutor simplificado para coordinación de particiones.
     * NO procesa datos internamente, solo coordina la distribución.
     */
    @Bean(name = "partitionCoordinatorTaskExecutor")
    public TaskExecutor partitionCoordinatorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración minimalista para coordinación
        executor.setCorePoolSize(1);           // 1 hilo coordinador por partition
        executor.setMaxPoolSize(4);            // Máximo 4 particiones concurrentes
        executor.setQueueCapacity(10);         // Cola pequeña para coordinación
        executor.setKeepAliveSeconds(30);
        
        executor.setThreadNamePrefix("Partition-Coordinator-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        System.out.println("🧩 Partition Coordinator TaskExecutor configurado:");
        System.out.println("   1 hilo coordinador por partition");
        System.out.println("   Máximo 4 particiones concurrentes");
        System.out.println("   SIN procesamiento interno de datos");
        System.out.println("   Estrategia: DISTRIBUCIÓN PURA");
        
        return executor;
    }

    /**
     * Simple AsyncTaskExecutor como fallback para tareas básicas.
     * Executor ligero para operaciones que no requieren pool complejo.
     */
    @Bean(name = "simpleAsyncTaskExecutor")
    public SimpleAsyncTaskExecutor simpleAsyncTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("SimpleAsync-");
        executor.setConcurrencyLimit(3);  // Límite de 3 hilos concurrentes
        
        System.out.println("Simple Async TaskExecutor configurado:");
        System.out.println("   Límite de concurrencia: 3 hilos");
        System.out.println("   Executor de fallback disponible");
        
        return executor;
    }

    /**
     * Configuración del tamaño de chunk optimizado para procesamiento paralelo.
     * Retorna el tamaño de chunk balanceado entre rendimiento y memoria.
     */
    @Bean(name = "optimizedChunkSize")
    public Integer getOptimizedChunkSize() {
        final int CHUNK_SIZE = 5;
        
        System.out.println("Chunk Size Optimizado configurado:");
        System.out.println("   Tamaño por chunk: " + CHUNK_SIZE + " registros");
        System.out.println("   Optimizado para 3 hilos paralelos");
        System.out.println("   Balance rendimiento/memoria alcanzado");
        
        return CHUNK_SIZE;
    }

    /**
     * Bean para métricas de escalamiento y monitoreo de rendimiento.
     */
    @Bean(name = "scalingMetrics")
    public ScalingMetrics scalingMetrics() {
        return new ScalingMetrics();
    }

    /**
     * Clase para métricas de escalamiento y análisis de rendimiento.
     */
    public static class ScalingMetrics {
        private long startTime;
        private long endTime;
        private int totalThreadsUsed = 0;
        private int totalChunksProcessed = 0;
        private int concurrentPeakThreads = 0;

        public void startMeasurement() {
            this.startTime = System.currentTimeMillis();
            System.out.println("=== INICIANDO MEDICIÓN DE ESCALAMIENTO PARALELO ===");
        }

        public void endMeasurement() {
            this.endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            System.out.println("\n=== REPORTE DE ESCALAMIENTO PARALELO ===");
            System.out.println("Tiempo total de ejecución: " + executionTime + "ms");
            System.out.println("Threads utilizados: " + totalThreadsUsed);
            System.out.println("Chunks procesados: " + totalChunksProcessed);
            System.out.println("Pico de concurrencia: " + concurrentPeakThreads + " hilos");
            System.out.printf("Throughput promedio: %.2f chunks/segundo%n", 
                (totalChunksProcessed * 1000.0) / executionTime);
            System.out.printf("Aceleración paralela: %.2fx%n", 
                (3.0 * totalChunksProcessed * 5) / (executionTime / 1000.0));
        }

        // Getters y Setters
        public void incrementThreadsUsed() { this.totalThreadsUsed++; }
        public void incrementChunksProcessed() { this.totalChunksProcessed++; }
        public void updateConcurrentPeak(int current) { 
            this.concurrentPeakThreads = Math.max(this.concurrentPeakThreads, current); 
        }
    }
}
