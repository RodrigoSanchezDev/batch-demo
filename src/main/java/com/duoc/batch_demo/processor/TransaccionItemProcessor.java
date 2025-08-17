package com.duoc.batch_demo.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.duoc.batch_demo.model.Transaccion;

/**
 * Procesador para transacciones diarias que detecta anomalías y valida datos
 */
@Component
public class TransaccionItemProcessor implements ItemProcessor<Transaccion, Transaccion> {

    private static final BigDecimal MONTO_MAXIMO_DIARIO = new BigDecimal("50000");
    private static final BigDecimal MONTO_MINIMO = new BigDecimal("0.01");

    @Override
    public Transaccion process(Transaccion transaccion) throws Exception {
        if (transaccion == null) {
            return null;
        }

        // Establecer fecha de procesamiento
        transaccion.setFechaProcesamiento(LocalDateTime.now());

        // Validar y limpiar datos
        boolean esAnomalia = false;
        StringBuilder motivosAnomalia = new StringBuilder();

        // Validar monto
        if (transaccion.getMonto() == null) {
            transaccion.setMonto(BigDecimal.ZERO);
            esAnomalia = true;
            motivosAnomalia.append("Monto nulo corregido a 0; ");
        } else if (transaccion.getMonto().compareTo(MONTO_MAXIMO_DIARIO) > 0) {
            esAnomalia = true;
            motivosAnomalia.append("Monto excede límite diario (").append(MONTO_MAXIMO_DIARIO).append("); ");
        } else if (transaccion.getMonto().compareTo(MONTO_MINIMO) < 0 && transaccion.getMonto().compareTo(BigDecimal.ZERO) != 0) {
            esAnomalia = true;
            motivosAnomalia.append("Monto menor al mínimo permitido; ");
        }

        // Validar tipo de transacción
        if (transaccion.getTipo() == null || transaccion.getTipo().trim().isEmpty()) {
            transaccion.setTipo("UNKNOWN");
            esAnomalia = true;
            motivosAnomalia.append("Tipo de transacción vacío; ");
        } else {
            // Normalizar tipo
            String tipoNormalizado = transaccion.getTipo().trim().toUpperCase();
            if (!tipoNormalizado.equals("DEBITO") && !tipoNormalizado.equals("CREDITO")) {
                if (tipoNormalizado.equals("DEBIT")) {
                    tipoNormalizado = "DEBITO";
                } else if (tipoNormalizado.equals("CREDIT")) {
                    tipoNormalizado = "CREDITO";
                } else {
                    esAnomalia = true;
                    motivosAnomalia.append("Tipo de transacción inválido: ").append(transaccion.getTipo()).append("; ");
                }
            }
            transaccion.setTipo(tipoNormalizado);
        }

        // Validar fecha
        if (transaccion.getFecha() == null) {
            transaccion.setFecha(LocalDate.now());
            esAnomalia = true;
            motivosAnomalia.append("Fecha nula corregida a fecha actual; ");
        } else if (transaccion.getFecha().isAfter(LocalDate.now())) {
            esAnomalia = true;
            motivosAnomalia.append("Fecha futura detectada; ");
        }

        // Validar ID
        if (transaccion.getId() == null || transaccion.getId() <= 0) {
            esAnomalia = true;
            motivosAnomalia.append("ID de transacción inválido; ");
        }

        // Establecer anomalía
        transaccion.setEsAnomalia(esAnomalia);
        if (esAnomalia) {
            transaccion.setMotivoAnomalia(motivosAnomalia.toString());
            System.out.println("🚨 ANOMALÍA DETECTADA - Transacción ID: " + transaccion.getId() + " - " + motivosAnomalia);
        }

        return transaccion;
    }
}
