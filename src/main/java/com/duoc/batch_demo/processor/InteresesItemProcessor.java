package com.duoc.batch_demo.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.duoc.batch_demo.model.Cuenta;

/**
 * Procesador para calcular intereses mensuales sobre cuentas
 */
@Component
public class InteresesItemProcessor implements ItemProcessor<Cuenta, Cuenta> {

    // Tasas de interés por tipo de cuenta
    private static final BigDecimal TASA_AHORRO = new BigDecimal("0.0225"); // 2.25% anual
    private static final BigDecimal TASA_PRESTAMO = new BigDecimal("-0.0850"); // -8.5% anual (se paga interés)
    private static final BigDecimal TASA_CORRIENTE = new BigDecimal("0.0050"); // 0.5% anual

    @Override
    public Cuenta process(Cuenta cuenta) throws Exception {
        if (cuenta == null) {
            return null;
        }

        // Validar y corregir datos
        if (cuenta.getSaldo() == null) {
            cuenta.setSaldo(BigDecimal.ZERO);
        }

        if (cuenta.getTipo() == null || cuenta.getTipo().trim().isEmpty()) {
            cuenta.setTipo("CORRIENTE"); // Tipo por defecto
        } else {
            // Normalizar tipo de cuenta
            String tipoNormalizado = normalizarTipoCuenta(cuenta.getTipo().trim().toUpperCase());
            cuenta.setTipo(tipoNormalizado);
        }

        if (cuenta.getNombre() == null || cuenta.getNombre().trim().isEmpty()) {
            cuenta.setNombre("CLIENTE DESCONOCIDO");
        } else {
            // Limpiar y normalizar nombre
            cuenta.setNombre(cuenta.getNombre().trim().toUpperCase());
        }

        // Validar edad
        if (cuenta.getEdad() == null || cuenta.getEdad() < 0 || cuenta.getEdad() > 120) {
            cuenta.setEdad(null); // Edad inválida
        }

        // Calcular interés mensual
        BigDecimal tasaInteres = obtenerTasaInteres(cuenta.getTipo());
        BigDecimal interesMensual = calcularInteresMensual(cuenta.getSaldo(), tasaInteres);
        
        // Actualizar saldo con interés
        BigDecimal nuevoSaldo = cuenta.getSaldo().add(interesMensual);
        cuenta.setSaldo(nuevoSaldo);
        cuenta.setFechaActualizacion(LocalDateTime.now());

        System.out.println("💰 INTERÉS CALCULADO - Cuenta: " + cuenta.getCuentaId() + 
                          " (" + cuenta.getTipo() + ") - Saldo anterior: " + cuenta.getSaldo().subtract(interesMensual) +
                          " - Interés: " + interesMensual + " - Nuevo saldo: " + nuevoSaldo);

        return cuenta;
    }

    private String normalizarTipoCuenta(String tipo) {
        return switch (tipo) {
            case "SAVINGS", "SAVING", "SAVE" -> "AHORRO";
            case "LOAN", "LENDING", "PRESTAMO" -> "PRESTAMO";
            case "CHECKING", "CURRENT", "CORRIENTE" -> "CORRIENTE";
            case "-1", "UNKNOWN" -> "CORRIENTE"; // Tipo por defecto para casos inválidos
            default -> tipo.equals("AHORRO") || tipo.equals("PRESTAMO") ? tipo : "CORRIENTE";
        };
    }

    private BigDecimal obtenerTasaInteres(String tipoCuenta) {
        return switch (tipoCuenta) {
            case "AHORRO" -> TASA_AHORRO;
            case "PRESTAMO" -> TASA_PRESTAMO;
            case "CORRIENTE" -> TASA_CORRIENTE;
            default -> TASA_CORRIENTE;
        };
    }

    private BigDecimal calcularInteresMensual(BigDecimal saldo, BigDecimal tasaAnual) {
        // Calcular interés mensual: (saldo * tasa_anual) / 12
        BigDecimal interesMensual = saldo.multiply(tasaAnual).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        return interesMensual;
    }
}
