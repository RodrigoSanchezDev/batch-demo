package com.duoc.batch_demo.config;

import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * Configuración de particiones para Spring Batch con H2 Database.
 * 
 * Proporciona PartitionHandlers optimizados para procesamiento distribuido
 * manteniendo compatibilidad con el sistema existente.
 * 
 * NOTA: Los PartitionHandlers se configuran inline en los Jobs para evitar
 * problemas de inicialización de contexto Spring.
 * 
 * @author Rodrigo Sanchez
 * @version 1.0
 */
@Configuration
public class PartitionConfig {
    
    /**
     * Método helper para crear PartitionHandler para transacciones.
     * Se utiliza desde los jobs para configuración inline.
     */
    public static PartitionHandler createTransactionPartitionHandler(TaskExecutor taskExecutor, org.springframework.batch.core.Step workerStep) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(taskExecutor);
        handler.setStep(workerStep);  // CRÍTICO: Asignar el worker step
        handler.setGridSize(4); // 4 particiones para transacciones
        
        System.out.println("🔧 Configurando TransactionPartitionHandler:");
        System.out.println("   • Particiones: 4");
        System.out.println("   • TaskExecutor: transactionTaskExecutor");
        System.out.println("   • Worker Step: " + workerStep.getName());
        System.out.println("   • Estrategia: Distribución por rangos de ID");
        
        return handler;
    }
    
    /**
     * Método helper para crear PartitionHandler para cuentas.
     * Se utiliza desde los jobs para configuración inline.
     */
    public static PartitionHandler createAccountPartitionHandler(TaskExecutor taskExecutor, org.springframework.batch.core.Step workerStep) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(taskExecutor);
        handler.setStep(workerStep);  // CRÍTICO: Asignar el worker step
        handler.setGridSize(3); // 3 particiones para cuentas
        
        System.out.println("🔧 Configurando AccountPartitionHandler:");
        System.out.println("   • Particiones: 3");
        System.out.println("   • TaskExecutor: accountTaskExecutor");
        System.out.println("   • Worker Step: " + workerStep.getName());
        System.out.println("   • Escalamiento: Dinámico");
        
        return handler;
    }
    
    /**
     * Método helper para crear PartitionHandler para anomalías.
     * Se utiliza desde los jobs para configuración inline.
     */
    public static PartitionHandler createAnomalyPartitionHandler(TaskExecutor taskExecutor, org.springframework.batch.core.Step workerStep) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(taskExecutor);
        handler.setStep(workerStep);  // CRÍTICO: Asignar el worker step
        handler.setGridSize(6); // 6 particiones para máximo rendimiento
        
        System.out.println("🔧 Configurando AnomalyPartitionHandler:");
        System.out.println("   • Particiones: 6");
        System.out.println("   • TaskExecutor: anomalyTaskExecutor");
        System.out.println("   • Worker Step: " + workerStep.getName());
        System.out.println("   • Rendimiento: Máximo paralelismo");
        
        return handler;
    }
    
    /**
     * Método helper para crear PartitionHandler general.
     * Se utiliza desde los jobs para configuración inline.
     */
    public static PartitionHandler createGeneralPartitionHandler(TaskExecutor taskExecutor) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(taskExecutor);
        handler.setGridSize(2); // 2 particiones para procesos generales
        
        System.out.println("🔧 Configurando GeneralPartitionHandler:");
        System.out.println("   • Particiones: 2");
        System.out.println("   • TaskExecutor: bankBatchTaskExecutor");
        System.out.println("   • Uso: Procesos batch generales");
        
        return handler;
    }
}
