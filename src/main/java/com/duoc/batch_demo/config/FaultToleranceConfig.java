package com.duoc.batch_demo.config;

import java.sql.SQLException;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * Configuración de tolerancia a fallos para procesamiento batch.
 * 
 * Implementa políticas personalizadas de reintento y omisión
 * para manejo robusto de errores en operaciones bancarias.
 * 
 * @author Rodrigo Sanchez
 * @version 1.1
 */
@Configuration
public class FaultToleranceConfig {

    // Políticas de reintento personalizadas
    
    /**
     * Política de reintento para transacciones bancarias.
     * Maneja diferentes tipos de excepciones con estrategias específicas.
     */
    @Bean
    public RetryPolicy transaccionesRetryPolicy() {
        // Política compuesta que combina múltiples estrategias
        CompositeRetryPolicy retryPolicy = new CompositeRetryPolicy();
        
        // Política basada en clasificación de excepciones
        ExceptionClassifierRetryPolicy exceptionPolicy = new ExceptionClassifierRetryPolicy();
        
        // Configurar reintentos específicos por tipo de excepción
        java.util.Map<Class<? extends Throwable>, RetryPolicy> policyMap = 
            new java.util.HashMap<>();
        
        // Errores de base de datos: 5 reintentos (problemas temporales de conectividad)
        SimpleRetryPolicy dbRetryPolicy = new SimpleRetryPolicy();
        dbRetryPolicy.setMaxAttempts(5);
        policyMap.put(SQLException.class, dbRetryPolicy);
        policyMap.put(DataAccessException.class, dbRetryPolicy);
        
        // 🔄 Errores de validación: 2 reintentos (datos potencialmente corregibles)
        SimpleRetryPolicy validationRetryPolicy = new SimpleRetryPolicy();
        validationRetryPolicy.setMaxAttempts(2);
        policyMap.put(ValidationException.class, validationRetryPolicy);
        
        // 🔄 Errores de parsing: 1 reintento (problemas de formato)
        SimpleRetryPolicy parseRetryPolicy = new SimpleRetryPolicy();
        parseRetryPolicy.setMaxAttempts(1);
        policyMap.put(FlatFileParseException.class, parseRetryPolicy);
        
        // 🔄 Errores runtime genéricos: 3 reintentos
        SimpleRetryPolicy runtimeRetryPolicy = new SimpleRetryPolicy();
        runtimeRetryPolicy.setMaxAttempts(3);
        policyMap.put(RuntimeException.class, runtimeRetryPolicy);
        
        exceptionPolicy.setPolicyMap(policyMap);
        retryPolicy.setPolicies(new RetryPolicy[]{exceptionPolicy});
        
        return retryPolicy;
    }
    
    /**
     * 🔄 Política de reintento para cuentas bancarias (más conservadora)
     */
    @Bean 
    public RetryPolicy cuentasRetryPolicy() {
        CompositeRetryPolicy retryPolicy = new CompositeRetryPolicy();
        ExceptionClassifierRetryPolicy exceptionPolicy = new ExceptionClassifierRetryPolicy();
        
        java.util.Map<Class<? extends Throwable>, RetryPolicy> policyMap = 
            new java.util.HashMap<>();
        
        // 🔄 Para cuentas, ser más conservadores con los reintentos
        SimpleRetryPolicy dbRetryPolicy = new SimpleRetryPolicy();
        dbRetryPolicy.setMaxAttempts(3); // Menos reintentos para cuentas
        policyMap.put(SQLException.class, dbRetryPolicy);
        policyMap.put(DataAccessException.class, dbRetryPolicy);
        
        SimpleRetryPolicy validationRetryPolicy = new SimpleRetryPolicy();
        validationRetryPolicy.setMaxAttempts(1);
        policyMap.put(ValidationException.class, validationRetryPolicy);
        
        SimpleRetryPolicy runtimeRetryPolicy = new SimpleRetryPolicy();
        runtimeRetryPolicy.setMaxAttempts(2);
        policyMap.put(RuntimeException.class, runtimeRetryPolicy);
        
        exceptionPolicy.setPolicyMap(policyMap);
        retryPolicy.setPolicies(new RetryPolicy[]{exceptionPolicy});
        
        return retryPolicy;
    }
    
    // ============================================
    // POLÍTICAS DE OMISIÓN PERSONALIZADAS
    // ============================================
    
    /**
     * ⏭️ Política de omisión inteligente para transacciones
     * Decide qué errores pueden saltarse y cuáles son críticos
     */
    @Bean
    public SkipPolicy transaccionesSkipPolicy() {
        return new SkipPolicy() {
            private int skipCount = 0;
            private final int maxSkipCount = 10; // Límite máximo personalizable
            
            @Override
            public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
                // 🚫 NUNCA omitir errores críticos de seguridad
                if (t instanceof SecurityException || t instanceof IllegalAccessException) {
                    System.err.println("🔒 ERROR CRÍTICO DE SEGURIDAD - NO SE PUEDE OMITIR: " + t.getMessage());
                    return false;
                }
                
                // 🚫 NUNCA omitir si se excede el límite personalizado
                if (this.skipCount >= maxSkipCount) {
                    System.err.println("⚠️ LÍMITE DE OMISIONES EXCEDIDO (" + maxSkipCount + ") - DETENIENDO");
                    throw new SkipLimitExceededException(maxSkipCount, t);
                }
                
                // ✅ PERMITIR omitir errores de validación de datos
                if (t instanceof ValidationException) {
                    this.skipCount++;
                    System.out.println("⏭️ OMITIENDO ERROR DE VALIDACIÓN (" + this.skipCount + "/" + maxSkipCount + "): " + t.getMessage());
                    return true;
                }
                
                // ✅ PERMITIR omitir errores de parsing de archivos
                if (t instanceof FlatFileParseException) {
                    this.skipCount++;
                    System.out.println("⏭️ OMITIENDO ERROR DE PARSING (" + this.skipCount + "/" + maxSkipCount + "): " + t.getMessage());
                    return true;
                }
                
                // ✅ PERMITIR omitir errores de datos faltantes (NumberFormatException, NullPointerException)
                if (t instanceof NumberFormatException || t instanceof NullPointerException) {
                    this.skipCount++;
                    System.out.println("⏭️ OMITIENDO ERROR DE DATOS (" + this.skipCount + "/" + maxSkipCount + "): " + t.getMessage());
                    return true;
                }
                
                // 🔍 EVALUAR otros errores caso por caso
                if (t instanceof RuntimeException) {
                    // Verificar si es un error recuperable
                    String message = t.getMessage();
                    if (message != null && (
                        message.contains("timeout") || 
                        message.contains("temporary") ||
                        message.contains("retry") ||
                        message.contains("connection"))) {
                        
                        this.skipCount++;
                        System.out.println("⏭️ OMITIENDO ERROR TEMPORAL (" + this.skipCount + "/" + maxSkipCount + "): " + message);
                        return true;
                    }
                }
                
                // 🚫 Por defecto, NO omitir errores desconocidos
                System.err.println("🛑 ERROR NO OMISIBLE: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                return false;
            }
        };
    }
    
    /**
     * ⏭️ Política de omisión más estricta para cuentas bancarias
     */
    @Bean
    public SkipPolicy cuentasSkipPolicy() {
        return new SkipPolicy() {
            private int skipCount = 0;
            private final int maxSkipCount = 5; // Límite más bajo para cuentas
            
            @Override
            public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
                // 🚫 Para cuentas, ser MÁS estrictos
                if (this.skipCount >= maxSkipCount) {
                    System.err.println("⚠️ LÍMITE DE OMISIONES PARA CUENTAS EXCEDIDO (" + maxSkipCount + ")");
                    throw new SkipLimitExceededException(maxSkipCount, t);
                }
                
                // 🚫 NO omitir errores de base de datos para cuentas
                if (t instanceof SQLException || t instanceof DataAccessException) {
                    System.err.println("🚫 ERROR DE BD EN CUENTAS - NO SE OMITE: " + t.getMessage());
                    return false;
                }
                
                // ✅ Solo omitir errores muy específicos y seguros
                if (t instanceof ValidationException && t.getMessage().contains("edad")) {
                    this.skipCount++;
                    System.out.println("⏭️ OMITIENDO VALIDACIÓN DE EDAD (" + this.skipCount + "/" + maxSkipCount + ")");
                    return true;
                }
                
                if (t instanceof NullPointerException && t.getMessage().contains("nombre")) {
                    this.skipCount++;
                    System.out.println("⏭️ OMITIENDO NOMBRE NULO (" + this.skipCount + "/" + maxSkipCount + ")");
                    return true;
                }
                
                // 🚫 Por defecto, NO omitir nada más para cuentas
                System.err.println("🛑 ERROR EN CUENTA NO OMISIBLE: " + t.getMessage());
                return false;
            }
        };
    }
    
    // ============================================
    // LISTENERS DE MONITOREO Y LOGGING
    // ============================================
    
    /**
     * 📊 Listener para monitorear reintentos y omisiones
     */
    @Bean
    public org.springframework.batch.core.StepExecutionListener faultToleranceListener() {
        return new org.springframework.batch.core.StepExecutionListener() {
            @Override
            public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
                System.out.println("🛡️ INICIANDO STEP CON POLÍTICAS DE TOLERANCIA A FALLOS: " + stepExecution.getStepName());
            }
            
            @Override
            public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
                System.out.println("📈 ESTADÍSTICAS DE TOLERANCIA A FALLOS para " + stepExecution.getStepName() + ":");
                System.out.println("   🔄 Rollbacks: " + stepExecution.getRollbackCount());
                System.out.println("   ⏭️ Registros omitidos (lectura): " + stepExecution.getReadSkipCount());
                System.out.println("   ⏭️ Registros omitidos (procesamiento): " + stepExecution.getProcessSkipCount());
                System.out.println("   ⏭️ Registros omitidos (escritura): " + stepExecution.getWriteSkipCount());
                System.out.println("   ✅ Commits exitosos: " + stepExecution.getCommitCount());
                
                // Determinar si el step fue exitoso a pesar de los errores
                if (stepExecution.getReadSkipCount() > 0 || stepExecution.getProcessSkipCount() > 0 || stepExecution.getWriteSkipCount() > 0) {
                    System.out.println("⚠️ STEP COMPLETADO CON OMISIONES - Revisar logs para detalles");
                } else {
                    System.out.println("✅ STEP COMPLETADO SIN ERRORES");
                }
                
                return stepExecution.getExitStatus();
            }
        };
    }
}
