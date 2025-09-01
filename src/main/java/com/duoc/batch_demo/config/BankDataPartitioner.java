package com.duoc.batch_demo.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Partitioner personalizado para distribuir el procesamiento de datos bancarios.
 * 
 * Divide el trabajo en particiones basadas en rangos de IDs para optimizar
 * el rendimiento con Spring Batch y bases de datos relacionales.
 * 
 * @author Rodrigo Sanchez
 * @version 1.1 - Corregido para que funcione correctamente con @StepScope
 */
@Component
public class BankDataPartitioner implements Partitioner {
    
    private static final String PARTITION_KEY = "partition";
    private static final String MIN_VALUE = "MIN_VALUE";
    private static final String MAX_VALUE = "MAX_VALUE";
    
    @Override
    @NonNull
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>(gridSize);
        
        // Rangos optimizados para datos pequeños/medianos
        int rangeSize = 25; // Cada partición maneja hasta 25 registros
        
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext value = new ExecutionContext();
            
            // Definir el rango de cada partición
            int minValue = i * rangeSize + 1;
            int maxValue = (i + 1) * rangeSize;
            
            // CRÍTICO: Usar las mismas claves que en @Value del reader (sin comillas)
            value.putInt(MIN_VALUE, minValue);
            value.putInt(MAX_VALUE, maxValue);
            value.putString(PARTITION_KEY, "partition" + i);
            
            result.put("partition" + i, value);
            
            System.out.println("🗂️  Configurando " + PARTITION_KEY + i + 
                             " -> Rango: " + minValue + "-" + maxValue);
            System.out.println("   📋 Contexto: " + MIN_VALUE + "=" + minValue + ", " + MAX_VALUE + "=" + maxValue);
        }
        
        System.out.println("✅ Total de particiones creadas: " + gridSize + 
                          " (Tamaño por partición: " + rangeSize + " registros)");
        
        return result;
    }
}
