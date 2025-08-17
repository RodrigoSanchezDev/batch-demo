package com.duoc.batch_demo.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;

import com.duoc.batch_demo.model.AnomaliaTransaccion;
import com.duoc.batch_demo.model.Transaccion;

public class AnomaliaTransaccionItemProcessor implements ItemProcessor<Transaccion, List<AnomaliaTransaccion>> {

    private static final BigDecimal MONTO_MINIMO = new BigDecimal("250.00");
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("50000.00");

    @Override
    public List<AnomaliaTransaccion> process(Transaccion transaccion) throws Exception {
        
        List<AnomaliaTransaccion> anomalias = new ArrayList<>();
        
        // Solo procesar transacciones que YA fueron marcadas como anómalas
        if (!transaccion.getEsAnomalia()) {
            return null; // No hay anomalías para esta transacción
        }
        
        BigDecimal monto = transaccion.getMonto().abs(); // Usar valor absoluto
        
        // Detectar diferentes tipos de anomalías
        if (monto.compareTo(MONTO_MINIMO) < 0) {
            AnomaliaTransaccion anomalia = new AnomaliaTransaccion(
                transaccion.getId(),
                "MONTO_MINIMO",
                "Transacción con monto menor al mínimo permitido (" + MONTO_MINIMO + ")",
                "ALTA"
            );
            anomalias.add(anomalia);
        }
        
        if (monto.compareTo(MONTO_MAXIMO) > 0) {
            AnomaliaTransaccion anomalia = new AnomaliaTransaccion(
                transaccion.getId(),
                "MONTO_MAXIMO",
                "Transacción con monto mayor al máximo permitido (" + MONTO_MAXIMO + ")",
                "CRÍTICA"
            );
            anomalias.add(anomalia);
        }
        
        // Detectar patrones sospechosos en débitos grandes
        if (transaccion.getMonto().compareTo(BigDecimal.ZERO) < 0 && 
            monto.compareTo(new BigDecimal("10000.00")) > 0) {
            AnomaliaTransaccion anomalia = new AnomaliaTransaccion(
                transaccion.getId(),
                "DEBITO_ALTO",
                "Débito de monto elevado que requiere revisión",
                "MEDIA"
            );
            anomalias.add(anomalia);
        }
        
        // Log para monitoreo
        for (AnomaliaTransaccion anomalia : anomalias) {
            System.out.println("🚨 ANOMALÍA REGISTRADA - Transacción ID: " + transaccion.getId() + 
                              " - Tipo: " + anomalia.getTipoAnomalia() + 
                              " - Severidad: " + anomalia.getSeveridad());
        }
        
        return anomalias.isEmpty() ? null : anomalias;
    }
}
