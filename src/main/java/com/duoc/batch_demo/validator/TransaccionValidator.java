package com.duoc.batch_demo.validator;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.stereotype.Component;

import com.duoc.batch_demo.model.Transaccion;

/**
 * 🔍 VALIDADOR PERSONALIZADO PARA TRANSACCIONES BANCARIAS
 * 
 * Implementa validaciones de negocio específicas para transacciones
 * con manejo inteligente de errores y tolerancia a fallos.
 * 
 * @author Rodrigo Sanchez
 * @version 1.0
 * @since 2025
 */
@Component
public class TransaccionValidator implements Validator<Transaccion> {

    private static final BigDecimal MONTO_MINIMO = new BigDecimal("0.01");
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("1000000.00");
    private static final int DIAS_MAXIMOS_ANTIGUEDAD = 365 * 5; // 5 años

    /**
     * 🔍 Valida una transacción bancaria aplicando reglas de negocio
     * 
     * @param transaccion La transacción a validar
     * @throws ValidationException Si la transacción no cumple las reglas
     */
    @Override
    public void validate(Transaccion transaccion) throws ValidationException {
        StringBuilder errores = new StringBuilder();
        
        // 1. VALIDACIÓN DE MONTO
        if (transaccion.getMonto() == null) {
            errores.append("Monto no puede ser nulo; ");
        } else {
            // Monto negativo
            if (transaccion.getMonto().compareTo(BigDecimal.ZERO) < 0) {
                errores.append("Monto no puede ser negativo (").append(transaccion.getMonto()).append("); ");
            }
            
            // Monto cero
            if (transaccion.getMonto().compareTo(BigDecimal.ZERO) == 0) {
                errores.append("Monto no puede ser cero; ");
            }
            
            // Monto muy pequeño
            if (transaccion.getMonto().compareTo(MONTO_MINIMO) < 0) {
                errores.append("Monto muy pequeño, mínimo ").append(MONTO_MINIMO).append("; ");
            }
            
            // Monto excesivo
            if (transaccion.getMonto().compareTo(MONTO_MAXIMO) > 0) {
                errores.append("Monto excesivo, máximo ").append(MONTO_MAXIMO).append("; ");
            }
        }
        
        // 2. VALIDACIÓN DE FECHA
        if (transaccion.getFecha() == null) {
            errores.append("Fecha no puede ser nula; ");
        } else {
            LocalDate hoy = LocalDate.now();
            
            // Fecha futura
            if (transaccion.getFecha().isAfter(hoy)) {
                errores.append("Fecha no puede ser futura (").append(transaccion.getFecha()).append("); ");
            }
            
            // Fecha muy antigua
            LocalDate fechaMinima = hoy.minusDays(DIAS_MAXIMOS_ANTIGUEDAD);
            if (transaccion.getFecha().isBefore(fechaMinima)) {
                errores.append("Fecha muy antigua, máximo ").append(DIAS_MAXIMOS_ANTIGUEDAD).append(" días; ");
            }
        }
        
        // 3. VALIDACIÓN DE TIPO
        if (transaccion.getTipo() == null || transaccion.getTipo().trim().isEmpty()) {
            errores.append("Tipo de transacción no puede estar vacío; ");
        } else {
            String tipo = transaccion.getTipo().toUpperCase().trim();
            if (!tipo.equals("DEBITO") && !tipo.equals("CREDITO")) {
                errores.append("Tipo debe ser DEBITO o CREDITO (recibido: ").append(tipo).append("); ");
            }
        }
        
        // 4. VALIDACIÓN DE ID
        if (transaccion.getId() == null || transaccion.getId() <= 0) {
            errores.append("ID de transacción debe ser un número positivo; ");
        }
        
        // 5. VALIDACIONES DE CONSISTENCIA LÓGICA
        if (transaccion.getTipo() != null && transaccion.getMonto() != null) {
            String tipo = transaccion.getTipo().toUpperCase().trim();
            
            // Los débitos normalmente serían negativos en algunos sistemas
            // pero en nuestro caso, validamos que los montos sean positivos
            if (tipo.equals("DEBITO") && transaccion.getMonto().compareTo(BigDecimal.ZERO) < 0) {
                errores.append("Débito con monto negativo puede ser inconsistente; ");
            }
            
            // Validar montos muy altos para ciertos tipos
            BigDecimal montoAltoCredito = new BigDecimal("100000.00");
            if (tipo.equals("CREDITO") && transaccion.getMonto().compareTo(montoAltoCredito) > 0) {
                errores.append("Crédito con monto muy alto requiere autorización especial; ");
            }
        }
        
        // Si hay errores, lanzar excepción de validación
        if (errores.length() > 0) {
            String mensajeError = "Errores de validación en transacción ID " + 
                                transaccion.getId() + ": " + errores.toString();
            
            System.out.println("❌ VALIDACIÓN FALLIDA: " + mensajeError);
            
            // Crear excepción personalizada con información detallada
            ValidationException exception = new ValidationException(mensajeError);
            exception.initCause(new IllegalArgumentException("Datos de transacción inválidos"));
            throw exception;
        }
        
        // Si llegamos aquí, la validación fue exitosa
        System.out.println("✅ TRANSACCIÓN VÁLIDA: ID " + transaccion.getId() + 
                         ", Tipo: " + transaccion.getTipo() + 
                         ", Monto: " + transaccion.getMonto());
    }
    
    /**
     * 📊 Proporciona estadísticas de validación
     * 
     * @param transaccion La transacción validada
     * @return String con información de la validación
     */
    public String obtenerEstadisticasValidacion(Transaccion transaccion) {
        if (transaccion == null) {
            return "Transacción nula - no se puede validar";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Estadísticas de validación para ID ").append(transaccion.getId()).append(":\n");
        stats.append("   💰 Monto: ").append(transaccion.getMonto()).append("\n");
        stats.append("   📅 Fecha: ").append(transaccion.getFecha()).append("\n");
        stats.append("   🏷️  Tipo: ").append(transaccion.getTipo()).append("\n");
        stats.append("   ✅ Estado: VÁLIDA");
        
        return stats.toString();
    }
}
