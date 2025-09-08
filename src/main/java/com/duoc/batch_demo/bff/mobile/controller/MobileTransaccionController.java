package com.duoc.batch_demo.bff.mobile.controller;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.batch_demo.dto.TransaccionMovilDTO;
import com.duoc.batch_demo.model.Transaccion;
import com.duoc.batch_demo.service.TransaccionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador BFF Móvil para transacciones.
 * Optimizado para dispositivos móviles con respuestas ligeras y cache agresivo.
 */
@RestController
@RequestMapping("/api/mobile/transacciones")
@Tag(name = "Mobile Transacciones", description = "APIs ligeras de transacciones para móviles")
public class MobileTransaccionController {

    @Autowired
    private TransaccionService transaccionService;

    private static final DateTimeFormatter MOBILE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    @Operation(summary = "Obtener transacciones recientes", 
               description = "Lista limitada de transacciones recientes optimizada para móviles")
    @GetMapping("/recientes")
    @PreAuthorize("hasRole('MOBILE')")
    public ResponseEntity<Map<String, Object>> obtenerTransaccionesRecientes(
            @Parameter(description = "Número máximo de transacciones (máx: 20)") 
            @RequestParam(defaultValue = "10") int limit) {

        // Limitar para optimizar ancho de banda móvil
        int limiteFinal = Math.min(limit, 20);
        List<Transaccion> transacciones = transaccionService.obtenerTransaccionesRecientes(limiteFinal);

        // Convertir a DTOs ligeros
        List<TransaccionMovilDTO> transaccionesMovil = transacciones.stream()
            .map(this::convertirAMovilDTO)
            .toList();

        // Respuesta optimizada para móvil
        Map<String, Object> response = new HashMap<>();
        response.put("transacciones", transaccionesMovil);
        response.put("total", transaccionesMovil.size());
        response.put("cache_duration", 300); // 5 minutos
        response.put("last_update", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Resumen rápido de transacciones", 
               description = "Resumen estadístico ultraligero para dashboard móvil")
    @GetMapping("/resumen")
    @PreAuthorize("hasRole('MOBILE')")
    public ResponseEntity<Map<String, Object>> obtenerResumen() {
        TransaccionService.TransaccionResumen resumen = transaccionService.obtenerResumenTransacciones();

        // Resumen ultraligero para móvil
        Map<String, Object> resumenMovil = new HashMap<>();
        resumenMovil.put("total", resumen.getTotalTransacciones());
        resumenMovil.put("hoy", resumen.getTransaccionesHoy());
        resumenMovil.put("balance", formatearBalance(resumen.getTotalCreditos(), resumen.getTotalDebitos()));
        resumenMovil.put("cache", 600); // Cache 10 minutos

        return ResponseEntity.ok(resumenMovil);
    }

    @Operation(summary = "Notificaciones de anomalías", 
               description = "Alertas push sobre anomalías detectadas")
    @GetMapping("/notificaciones")
    @PreAuthorize("hasRole('MOBILE')")
    public ResponseEntity<Map<String, Object>> obtenerNotificaciones() {
        List<Transaccion> anomalias = transaccionService.obtenerAnomalias();

        // Solo las más recientes y críticas
        List<Map<String, Object>> notificaciones = anomalias.stream()
            .limit(5) // Máximo 5 notificaciones
            .map(this::convertirANotificacion)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificaciones);
        response.put("count", notificaciones.size());
        response.put("priority", determinarPrioridad(anomalias.size()));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Transacciones por fecha", 
               description = "Transacciones de un día específico")
    @GetMapping("/fecha/{fecha}")
    @PreAuthorize("hasRole('MOBILE')")
    public ResponseEntity<Map<String, Object>> obtenerTransaccionesPorFecha(
            @Parameter(description = "Fecha en formato yyyy-MM-dd") 
            @PathVariable String fecha) {

        try {
            // En un sistema real, parsearías la fecha y filtrarías
            List<Transaccion> transacciones = transaccionService.obtenerTransaccionesRecientes(15);
            
            List<TransaccionMovilDTO> transaccionesMovil = transacciones.stream()
                .map(this::convertirAMovilDTO)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("fecha", fecha);
            response.put("transacciones", transaccionesMovil);
            response.put("total_dia", transaccionesMovil.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Formato de fecha inválido"));
        }
    }

    @Operation(summary = "Quick stats", 
               description = "Estadísticas ultrarrápidas para widget móvil")
    @GetMapping("/quick-stats")
    @PreAuthorize("hasRole('MOBILE')")
    public ResponseEntity<Map<String, String>> obtenerQuickStats() {
        TransaccionService.TransaccionResumen resumen = transaccionService.obtenerResumenTransacciones();

        // Solo strings formateados para mostrar directamente
        Map<String, String> stats = new HashMap<>();
        stats.put("total", resumen.getTotalTransacciones().toString());
        stats.put("hoy", resumen.getTransaccionesHoy().toString());
        stats.put("creditos", formatearMonto(resumen.getTotalCreditos()));
        stats.put("debitos", formatearMonto(resumen.getTotalDebitos()));

        return ResponseEntity.ok(stats);
    }

    // Métodos auxiliares para optimización móvil

    private TransaccionMovilDTO convertirAMovilDTO(Transaccion transaccion) {
        String fechaFormateada = transaccion.getFecha() != null ? 
            transaccion.getFecha().format(MOBILE_DATE_FORMAT) : "N/A";
            
        return new TransaccionMovilDTO(
            transaccion.getId(),
            fechaFormateada,
            transaccion.getMonto(),
            transaccion.getTipo(),
            transaccion.getEsAnomalia()
        );
    }

    private Map<String, Object> convertirANotificacion(Transaccion anomalia) {
        Map<String, Object> notificacion = new HashMap<>();
        notificacion.put("id", anomalia.getId());
        notificacion.put("mensaje", generarMensajeNotificacion(anomalia));
        notificacion.put("tipo", "anomalia");
        notificacion.put("timestamp", System.currentTimeMillis());
        return notificacion;
    }

    private String generarMensajeNotificacion(Transaccion anomalia) {
        if (anomalia.getMonto().compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "Transacción con monto $0 detectada";
        } else if (anomalia.getMonto().compareTo(new java.math.BigDecimal("10000")) > 0) {
            return "Transacción de alto monto detectada";
        } else {
            return "Anomalía detectada en transacción";
        }
    }

    private String formatearBalance(java.math.BigDecimal creditos, java.math.BigDecimal debitos) {
        if (creditos == null) creditos = java.math.BigDecimal.ZERO;
        if (debitos == null) debitos = java.math.BigDecimal.ZERO;
        
        java.math.BigDecimal balance = creditos.subtract(debitos);
        return formatearMonto(balance);
    }

    private String formatearMonto(java.math.BigDecimal monto) {
        if (monto == null) return "$0";
        return String.format("$%,.0f", monto); // Sin decimales para móvil
    }

    private String determinarPrioridad(int cantidadAnomalias) {
        if (cantidadAnomalias > 10) return "high";
        if (cantidadAnomalias > 5) return "medium";
        return "low";
    }
}
