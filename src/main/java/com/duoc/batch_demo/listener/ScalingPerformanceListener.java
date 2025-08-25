package com.duoc.batch_demo.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import com.duoc.batch_demo.config.ScalingPolicyConfig.ScalingMetrics;

/**
 * Listener de rendimiento para escalamiento paralelo.
 * 
 * Monitorea y analiza el rendimiento del procesamiento
 * paralelo con múltiples hilos de ejecución.
 * 
 * @author Rodrigo Sanchez
 * @version 1.1
 */
@Component("scalingPerformanceListener")
public class ScalingPerformanceListener implements StepExecutionListener {

    private long stepStartTime;
    private long stepEndTime;
    private final ScalingMetrics scalingMetrics;
    
    public ScalingPerformanceListener(ScalingMetrics scalingMetrics) {
        this.scalingMetrics = scalingMetrics;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepStartTime = System.currentTimeMillis();
        String stepName = stepExecution.getStepName();
        
        System.out.println("\n🚀 === INICIANDO ESCALAMIENTO PARALELO: " + stepName + " ===");
        System.out.println("⏱️  Tiempo de inicio: " + new java.util.Date(stepStartTime));
        System.out.println("🧵 Configuración de threads: 3 paralelos");
        System.out.println("📦 Chunk size: 5 registros");
        System.out.println("🎯 Política de escalamiento: Activa");
        
        // Iniciar medición de métricas
        scalingMetrics.startMeasurement();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        stepEndTime = System.currentTimeMillis();
        long executionTime = stepEndTime - stepStartTime;
        
        String stepName = stepExecution.getStepName();
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long skipCount = stepExecution.getSkipCount();
        long processSkipCount = stepExecution.getProcessSkipCount();
        long writeSkipCount = stepExecution.getWriteSkipCount();
        
        // Actualizar métricas de escalamiento
        scalingMetrics.incrementChunksProcessed();
        scalingMetrics.endMeasurement();
        
        System.out.println("\n📈 === REPORTE DE ESCALAMIENTO PARALELO: " + stepName + " ===");
        System.out.println("⏱️  Tiempo total de ejecución: " + executionTime + "ms");
        System.out.println("📊 Registros leídos: " + readCount);
        System.out.println("📊 Registros escritos: " + writeCount);
        System.out.println("📊 Registros omitidos: " + skipCount);
        
        if (skipCount > 0) {
            System.out.println("   ├─ Process skips: " + processSkipCount);
            System.out.println("   └─ Write skips: " + writeSkipCount);
        }
        
        // === CÁLCULO DE MÉTRICAS DE RENDIMIENTO PARALELO ===
        double throughputPerSecond = readCount > 0 ? (readCount * 1000.0) / executionTime : 0;
        double chunksPerSecond = readCount > 0 ? (Math.ceil(readCount / 5.0) * 1000.0) / executionTime : 0;
        
        // Estimación de aceleración paralela (comparado con secuencial)
        double estimatedSequentialTime = readCount * 10; // 10ms por registro estimado
        double parallelAcceleration = estimatedSequentialTime / executionTime;
        
        System.out.println("🚀 === ANÁLISIS DE RENDIMIENTO PARALELO ===");
        System.out.printf("⚡ Throughput: %.2f registros/segundo%n", throughputPerSecond);
        System.out.printf("📦 Chunks procesados: %.0f chunks/segundo%n", chunksPerSecond);
        System.out.printf("🎯 Aceleración paralela estimada: %.2fx%n", parallelAcceleration);
        System.out.printf("🧵 Eficiencia de threads: %.1f%%%n", (parallelAcceleration / 3) * 100);
        
        // === ANÁLISIS DE CALIDAD DEL PROCESAMIENTO ===
        double successRate = readCount > 0 ? ((double)(writeCount) / readCount) * 100 : 100;
        double skipRate = readCount > 0 ? ((double)skipCount / readCount) * 100 : 0;
        
        System.out.println("📈 === MÉTRICAS DE CALIDAD ===");
        System.out.printf("✅ Tasa de éxito: %.1f%%%n", successRate);
        System.out.printf("⚠️  Tasa de omisión: %.1f%%%n", skipRate);
        
        if (successRate >= 95.0) {
            System.out.println("🏆 EXCELENTE: Alta tasa de éxito en procesamiento paralelo");
        } else if (successRate >= 85.0) {
            System.out.println("👍 BUENO: Tasa de éxito aceptable");
        } else {
            System.out.println("⚠️  ATENCIÓN: Tasa de éxito por debajo del objetivo");
        }
        
        // === RECOMENDACIONES DE ESCALAMIENTO ===
        if (parallelAcceleration < 2.0) {
            System.out.println("💡 RECOMENDACIÓN: Considerar optimizar el procesamiento para mayor paralelismo");
        } else if (parallelAcceleration > 2.8) {
            System.out.println("🎉 EXCELENTE: Escalamiento paralelo muy eficiente");
        } else {
            System.out.println("👍 BUENO: Escalamiento paralelo funcionando correctamente");
        }
        
        System.out.println("🔚 Escalamiento paralelo completado: " + stepName);
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        return stepExecution.getExitStatus();
    }

    /**
     * 🎯 MÉTODO PARA OBTENER MÉTRICAS DETALLADAS
     */
    public ScalingMetrics getScalingMetrics() {
        return scalingMetrics;
    }

    /**
     * 🎯 MÉTODO PARA RESET DE MÉTRICAS ENTRE PASOS
     */
    public void resetMetrics() {
        // Las métricas se resetean automáticamente en cada step
        System.out.println("🔄 Métricas de escalamiento reseteadas para el siguiente step");
    }
}
