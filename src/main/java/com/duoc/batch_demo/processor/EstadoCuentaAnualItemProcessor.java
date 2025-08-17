package com.duoc.batch_demo.processor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;

import com.duoc.batch_demo.model.CuentaAnual;
import com.duoc.batch_demo.model.EstadoCuentaAnual;

public class EstadoCuentaAnualItemProcessor implements ItemProcessor<CuentaAnual, EstadoCuentaAnual> {

    // Mapa para agrupar transacciones por cuenta
    private final Map<Long, EstadoCuentaAnual> estadosPorCuenta = new HashMap<>();

    @Override
    public EstadoCuentaAnual process(CuentaAnual cuentaAnual) throws Exception {
        
        Long cuentaId = cuentaAnual.getCuentaId();
        
        // Obtener o crear el estado de cuenta para esta cuenta
        EstadoCuentaAnual estado = estadosPorCuenta.computeIfAbsent(cuentaId, id -> {
            EstadoCuentaAnual nuevoEstado = new EstadoCuentaAnual(cuentaId, "CUENTA_" + cuentaId);
            // Para implementación completa se integraría con servicio de cuentas
            return nuevoEstado;
        });

        // Procesar la transacción actual
        BigDecimal monto = cuentaAnual.getMonto();
        String tipoTransaccion = cuentaAnual.getTransaccion().toUpperCase();
        
        // Actualizar contadores según tipo de transacción
        switch (tipoTransaccion) {
            case "DEPOSITO":
                if (monto.compareTo(BigDecimal.ZERO) > 0) {
                    estado.setTotalDepositos(estado.getTotalDepositos().add(monto));
                }
                break;
                
            case "RETIRO":
            case "DEBITO":
                if (monto.compareTo(BigDecimal.ZERO) < 0) {
                    estado.setTotalRetiros(estado.getTotalRetiros().add(monto.abs()));
                }
                break;
                
            case "COMPRA":
                if (monto.compareTo(BigDecimal.ZERO) < 0) {
                    estado.setTotalRetiros(estado.getTotalRetiros().add(monto.abs()));
                }
                break;
        }
        
        // Incrementar contador de transacciones
        estado.setTotalTransacciones(estado.getTotalTransacciones() + 1);
        
        // Calcular saldo final (simplificado)
        BigDecimal saldoFinal = estado.getTotalDepositos().subtract(estado.getTotalRetiros());
        estado.setSaldoFinal(saldoFinal);
        
        // Estimar intereses ganados (1% del saldo final si es positivo)
        if (saldoFinal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal interesesEstimados = saldoFinal.multiply(new BigDecimal("0.01"));
            estado.setInteresesGanados(interesesEstimados);
        }
        
        // Log para monitoreo
        System.out.println("📋 ESTADO CUENTA PROCESADO - Cuenta ID: " + cuentaId + 
                          " - Total Depósitos: " + estado.getTotalDepositos() + 
                          " - Total Retiros: " + estado.getTotalRetiros() + 
                          " - Saldo Final: " + estado.getSaldoFinal() + 
                          " - Transacciones: " + estado.getTotalTransacciones());
        
        // Retornar una copia del estado actual
        EstadoCuentaAnual estadoCopia = new EstadoCuentaAnual(estado.getCuentaId(), estado.getNombre());
        estadoCopia.setTotalDepositos(estado.getTotalDepositos());
        estadoCopia.setTotalRetiros(estado.getTotalRetiros());
        estadoCopia.setSaldoFinal(estado.getSaldoFinal());
        estadoCopia.setTotalTransacciones(estado.getTotalTransacciones());
        estadoCopia.setInteresesGanados(estado.getInteresesGanados());
        estadoCopia.setAño(2024); // Año de las transacciones
        
        return estadoCopia;
    }
}
