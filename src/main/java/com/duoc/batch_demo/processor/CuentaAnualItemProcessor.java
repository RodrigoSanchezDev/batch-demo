package com.duoc.batch_demo.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.duoc.batch_demo.model.CuentaAnual;

/**
 * Procesador para datos anuales de cuentas que limpia y valida información para auditorías
 */
@Component
public class CuentaAnualItemProcessor implements ItemProcessor<CuentaAnual, CuentaAnual> {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    @Override
    public CuentaAnual process(CuentaAnual cuentaAnual) throws Exception {
        if (cuentaAnual == null) {
            return null;
        }

        // Establecer fecha de procesamiento
        cuentaAnual.setFechaProcesamiento(LocalDateTime.now());

        // Validar y corregir cuenta ID
        if (cuentaAnual.getCuentaId() == null || cuentaAnual.getCuentaId() <= 0) {
            System.out.println("⚠️  ADVERTENCIA - Cuenta ID inválido: " + cuentaAnual.getCuentaId());
            return null; // Descartar registros con ID inválido
        }

        // Validar y normalizar tipo de transacción
        if (cuentaAnual.getTransaccion() == null || cuentaAnual.getTransaccion().trim().isEmpty()) {
            cuentaAnual.setTransaccion("DESCONOCIDA");
        } else {
            String transaccionNormalizada = normalizarTipoTransaccion(cuentaAnual.getTransaccion().trim().toUpperCase());
            cuentaAnual.setTransaccion(transaccionNormalizada);
        }

        // Validar y corregir monto
        if (cuentaAnual.getMonto() == null) {
            cuentaAnual.setMonto(BigDecimal.ZERO);
            System.out.println("⚠️  ADVERTENCIA - Monto nulo corregido a 0 para cuenta: " + cuentaAnual.getCuentaId());
        }

        // Limpiar descripción
        if (cuentaAnual.getDescripcion() == null) {
            cuentaAnual.setDescripcion("Sin descripción");
        } else {
            cuentaAnual.setDescripcion(cuentaAnual.getDescripcion().trim());
            if (cuentaAnual.getDescripcion().isEmpty()) {
                cuentaAnual.setDescripcion("Sin descripción");
            }
        }

        // Validar fecha - si es inválida, usar fecha actual
        if (cuentaAnual.getFecha() == null) {
            cuentaAnual.setFecha(LocalDate.now());
            System.out.println("⚠️  ADVERTENCIA - Fecha nula corregida para cuenta: " + cuentaAnual.getCuentaId());
        }

        System.out.println("📋 PROCESADO - Cuenta Anual ID: " + cuentaAnual.getCuentaId() + 
                          " - " + cuentaAnual.getTransaccion() + 
                          " - Monto: " + cuentaAnual.getMonto() + 
                          " - Fecha: " + cuentaAnual.getFecha());

        return cuentaAnual;
    }

    private String normalizarTipoTransaccion(String transaccion) {
        return switch (transaccion) {
            case "DEPOSIT", "DEPOSITS", "DEPOSITO", "DEPOSITOS" -> "DEPOSITO";
            case "WITHDRAWAL", "WITHDRAW", "RETIRO", "RETIROS" -> "RETIRO";
            case "TRANSFER", "TRANSFERENCIA", "TRANSFERS" -> "TRANSFERENCIA";
            case "PAYMENT", "PAGO", "PAGOS" -> "PAGO";
            case "INTEREST", "INTERES", "INTERESES" -> "INTERES";
            default -> transaccion;
        };
    }

    /**
     * Intenta parsear una fecha usando múltiples formatos
     */
    private LocalDate parsearFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.trim().isEmpty()) {
            return null;
        }

        fechaStr = fechaStr.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(fechaStr, formatter);
            } catch (DateTimeParseException e) {
                // Continuar con el siguiente formato
            }
        }

        System.out.println("⚠️  ADVERTENCIA - No se pudo parsear la fecha: " + fechaStr);
        return null;
    }
}
