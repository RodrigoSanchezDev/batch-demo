package com.duoc.batch_demo.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.batch.item.ItemProcessor;

import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.InteresCalculado;

public class InteresCalculadoItemProcessor implements ItemProcessor<Cuenta, InteresCalculado> {

    @Override
    public InteresCalculado process(Cuenta cuenta) throws Exception {
        
        // Obtener saldo anterior (antes del cálculo)
        BigDecimal saldoAnterior = cuenta.getSaldo();
        
        // Determinar tasa de interés según tipo de cuenta
        BigDecimal tasaInteres;
        switch (cuenta.getTipo().toUpperCase()) {
            case "AHORRO":
                tasaInteres = new BigDecimal("0.01875"); // 1.875% mensual
                break;
            case "CORRIENTE":
                tasaInteres = new BigDecimal("0.00417"); // 0.417% mensual
                break;
            case "PRESTAMO":
                tasaInteres = new BigDecimal("-0.07083"); // -7.083% mensual (cargo por interés)
                break;
            default:
                tasaInteres = BigDecimal.ZERO;
                break;
        }
        
        // Calcular interés
        BigDecimal interesCalculado = saldoAnterior.multiply(tasaInteres)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calcular nuevo saldo
        BigDecimal saldoNuevo = saldoAnterior.add(interesCalculado);
        
        // Actualizar el saldo de la cuenta
        cuenta.setSaldo(saldoNuevo);
        cuenta.setFechaActualizacion(LocalDateTime.now());
        
        // Crear el registro de detalle del interés calculado
        InteresCalculado detalle = new InteresCalculado(
            cuenta.getCuentaId(),
            saldoAnterior,
            tasaInteres,
            interesCalculado,
            saldoNuevo,
            cuenta.getTipo()
        );
        
        // Log para monitoreo
        System.out.println("💰 INTERÉS CALCULADO - Cuenta: " + cuenta.getCuentaId() + 
                          " (" + cuenta.getTipo() + ") - Saldo anterior: " + saldoAnterior + 
                          " - Interés: " + interesCalculado + " - Nuevo saldo: " + saldoNuevo);
        
        return detalle;
    }
}
