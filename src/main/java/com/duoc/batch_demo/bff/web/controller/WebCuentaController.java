package com.duoc.batch_demo.bff.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.service.CuentaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador BFF Web para gestión de cuentas bancarias.
 * Proporciona APIs completas para administración de cuentas desde interfaces web.
 */
@RestController
@RequestMapping("/api/web/cuentas")
@Tag(name = "Web Cuentas", description = "APIs de cuentas para clientes web")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class WebCuentaController {

    @Autowired
    private CuentaService cuentaService;

    @Operation(summary = "Obtener todas las cuentas con paginación", 
               description = "Lista paginada de cuentas bancarias con información completa")
    @GetMapping
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> obtenerCuentas(
            @Parameter(description = "Número de página (0-indexed)") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Tamaño de página") 
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Cuenta> cuentas = cuentaService.obtenerCuentasPaginadas(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("cuentas", cuentas.getContent());
        response.put("pagination", Map.of(
            "current_page", cuentas.getNumber(),
            "total_pages", cuentas.getTotalPages(),
            "total_elements", cuentas.getTotalElements(),
            "size", cuentas.getSize(),
            "first", cuentas.isFirst(),
            "last", cuentas.isLast()
        ));
        response.put("summary", obtenerResumenPagina(cuentas.getContent()));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener detalle completo de una cuenta", 
               description = "Información detallada de una cuenta específica incluyendo historial")
    @GetMapping("/{cuentaId}")
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> obtenerDetalleCuenta(
            @Parameter(description = "ID de la cuenta") 
            @PathVariable Long cuentaId,
            
            @Parameter(description = "Incluir historial de transacciones") 
            @RequestParam(defaultValue = "true") boolean includeHistorial) {

        Optional<Cuenta> cuentaOpt = cuentaService.obtenerCuentaPorId(cuentaId);
        
        if (cuentaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Cuenta cuenta = cuentaOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("cuenta", cuenta);
        response.put("metadata", Map.of(
            "account_age_group", clasificarPorEdad(cuenta.getEdad()),
            "account_type", cuenta.getTipo(),
            "balance_status", clasificarSaldo(cuenta.getSaldo()),
            "last_updated", System.currentTimeMillis()
        ));

        // Si se requiere historial, agregarlo
        if (includeHistorial) {
            // En un sistema real, aquí obtendrías las transacciones de esta cuenta
            response.put("historial_disponible", true);
            response.put("nota", "Historial de transacciones disponible via /api/web/transacciones");
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener cuentas por tipo", 
               description = "Filtrar cuentas por tipo (ahorro, prestamo, hipoteca)")
    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> obtenerCuentasPorTipo(
            @Parameter(description = "Tipo de cuenta") 
            @PathVariable String tipo) {

        List<Cuenta> cuentas = cuentaService.obtenerCuentasPorTipo(tipo);

        Map<String, Object> response = new HashMap<>();
        response.put("tipo_filtro", tipo);
        response.put("total_cuentas", cuentas.size());
        response.put("cuentas", cuentas);
        response.put("estadisticas", calcularEstadisticasPorTipo(cuentas));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener dashboard de cuentas", 
               description = "Resumen ejecutivo y estadísticas de todas las cuentas")
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> obtenerDashboard() {
        CuentaService.CuentaResumen resumen = cuentaService.obtenerResumenCuentas();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("resumen_general", resumen);
        dashboard.put("distribucion_tipos", Map.of(
            "ahorro", resumen.getCuentasAhorro(),
            "prestamo", resumen.getCuentasPrestamo(),
            "hipoteca", resumen.getCuentasHipoteca()
        ));
        dashboard.put("metricas_financieras", Map.of(
            "saldo_total_formateado", formatearMonto(resumen.getSaldoTotal()),
            "saldo_promedio_formateado", formatearMonto(resumen.getSaldoPromedio()),
            "cuentas_activas", resumen.getTotalCuentas()
        ));
        dashboard.put("timestamp", System.currentTimeMillis());
        dashboard.put("periodo_analisis", "Datos actuales del sistema");

        return ResponseEntity.ok(dashboard);
    }

    @Operation(summary = "Buscar cuentas con criterios avanzados", 
               description = "Búsqueda avanzada de cuentas con múltiples filtros")
    @PostMapping("/search")
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> buscarCuentas(
            @RequestBody CuentaSearchRequest searchRequest) {

        // En un sistema real, implementarías búsqueda avanzada en el servicio
        // Por ahora, simulamos con filtro por tipo si está presente
        List<Cuenta> resultados;
        
        if (searchRequest.getTipo() != null && !searchRequest.getTipo().isEmpty()) {
            resultados = cuentaService.obtenerCuentasPorTipo(searchRequest.getTipo());
        } else {
            // Obtener todas las cuentas (primera página)
            Page<Cuenta> page = cuentaService.obtenerCuentasPaginadas(PageRequest.of(0, 100));
            resultados = page.getContent();
        }

        // Filtros adicionales en memoria (en producción se haría en la consulta SQL)
        if (searchRequest.getEdadMinima() != null) {
            resultados = resultados.stream()
                .filter(c -> c.getEdad() >= searchRequest.getEdadMinima())
                .toList();
        }

        if (searchRequest.getEdadMaxima() != null) {
            resultados = resultados.stream()
                .filter(c -> c.getEdad() <= searchRequest.getEdadMaxima())
                .toList();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("criterios_busqueda", searchRequest);
        response.put("total_resultados", resultados.size());
        response.put("resultados", resultados);
        response.put("estadisticas_resultados", calcularEstadisticasPorTipo(resultados));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Exportar cuentas", 
               description = "Exportar datos de cuentas para reportes")
    @GetMapping("/export")
    @PreAuthorize("hasRole('WEB') and hasAuthority('EXPORT_REPORTS')")
    public ResponseEntity<Map<String, Object>> exportarCuentas(
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "500") int limit) {

        List<Cuenta> cuentas;
        
        if (tipo != null && !tipo.isEmpty()) {
            cuentas = cuentaService.obtenerCuentasPorTipo(tipo);
        } else {
            Page<Cuenta> page = cuentaService.obtenerCuentasPaginadas(
                PageRequest.of(0, Math.min(limit, 1000))
            );
            cuentas = page.getContent();
        }

        Map<String, Object> export = new HashMap<>();
        export.put("export_timestamp", System.currentTimeMillis());
        export.put("total_records", cuentas.size());
        export.put("filters_applied", Map.of("tipo", tipo, "limit", limit));
        export.put("data", cuentas);
        export.put("summary", calcularEstadisticasPorTipo(cuentas));

        return ResponseEntity.ok(export);
    }

    // Métodos auxiliares

    private Map<String, Object> obtenerResumenPagina(List<Cuenta> cuentas) {
        return Map.of(
            "total_saldo_pagina", cuentas.stream()
                .map(Cuenta::getSaldo)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
            "edad_promedio", cuentas.stream()
                .mapToInt(Cuenta::getEdad)
                .average()
                .orElse(0),
            "tipos_representados", cuentas.stream()
                .map(Cuenta::getTipo)
                .distinct()
                .count()
        );
    }

    private String clasificarPorEdad(Integer edad) {
        if (edad == null) return "No especificada";
        if (edad < 30) return "Joven";
        if (edad < 50) return "Adulto";
        if (edad < 65) return "Adulto Mayor";
        return "Senior";
    }

    private String clasificarSaldo(java.math.BigDecimal saldo) {
        if (saldo == null) return "No disponible";
        if (saldo.compareTo(java.math.BigDecimal.ZERO) == 0) return "Sin saldo";
        if (saldo.compareTo(new java.math.BigDecimal("1000")) < 0) return "Saldo bajo";
        if (saldo.compareTo(new java.math.BigDecimal("10000")) < 0) return "Saldo medio";
        return "Saldo alto";
    }

    private String formatearMonto(java.math.BigDecimal monto) {
        if (monto == null) return "$0";
        return String.format("$%,.2f", monto);
    }

    private Map<String, Object> calcularEstadisticasPorTipo(List<Cuenta> cuentas) {
        Map<String, Long> conteoTipos = cuentas.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Cuenta::getTipo, 
                java.util.stream.Collectors.counting()
            ));

        java.math.BigDecimal saldoTotal = cuentas.stream()
            .map(Cuenta::getSaldo)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return Map.of(
            "conteo_por_tipo", conteoTipos,
            "saldo_total", saldoTotal,
            "cuenta_con_mayor_saldo", cuentas.stream()
                .max(java.util.Comparator.comparing(Cuenta::getSaldo))
                .map(Cuenta::getCuentaId)
                .orElse(-1L)
        );
    }

    // DTO para búsqueda avanzada
    public static class CuentaSearchRequest {
        private String tipo;
        private Integer edadMinima;
        private Integer edadMaxima;
        private java.math.BigDecimal saldoMinimo;
        private java.math.BigDecimal saldoMaximo;
        private String ordenarPor; // saldo, edad, nombre

        // Getters y Setters
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        
        public Integer getEdadMinima() { return edadMinima; }
        public void setEdadMinima(Integer edadMinima) { this.edadMinima = edadMinima; }
        
        public Integer getEdadMaxima() { return edadMaxima; }
        public void setEdadMaxima(Integer edadMaxima) { this.edadMaxima = edadMaxima; }
        
        public java.math.BigDecimal getSaldoMinimo() { return saldoMinimo; }
        public void setSaldoMinimo(java.math.BigDecimal saldoMinimo) { this.saldoMinimo = saldoMinimo; }
        
        public java.math.BigDecimal getSaldoMaximo() { return saldoMaximo; }
        public void setSaldoMaximo(java.math.BigDecimal saldoMaximo) { this.saldoMaximo = saldoMaximo; }
        
        public String getOrdenarPor() { return ordenarPor; }
        public void setOrdenarPor(String ordenarPor) { this.ordenarPor = ordenarPor; }
    }
}
