package com.duoc.batch_demo.bff.web.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.batch_demo.model.Transaccion;
import com.duoc.batch_demo.service.TransaccionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador BFF Web para gestión de transacciones.
 * Optimizado para navegadores con datos completos y funcionalidades avanzadas.
 */
@RestController
@RequestMapping("/api/web/transacciones")
@Tag(name = "Web Transacciones", description = "APIs de transacciones para clientes web")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class WebTransaccionController {

    @Autowired
    private TransaccionService transaccionService;

    @Operation(summary = "Obtener transacciones con paginación", 
               description = "Lista paginada de transacciones con filtros avanzados para interfaces web")
    @GetMapping
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> obtenerTransacciones(
            @Parameter(description = "Número de página (0-indexed)") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Tamaño de página") 
            @RequestParam(defaultValue = "50") int size,
            
            @Parameter(description = "Fecha desde (formato: yyyy-MM-dd)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            
            @Parameter(description = "Fecha hasta (formato: yyyy-MM-dd)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            
            @Parameter(description = "Tipo de transacción (credito/debito)") 
            @RequestParam(required = false) String tipo,
            
            @Parameter(description = "Monto mínimo") 
            @RequestParam(required = false) BigDecimal montoMinimo,
            
            @Parameter(description = "Monto máximo") 
            @RequestParam(required = false) BigDecimal montoMaximo) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Máximo 100 por página
        
        Page<Transaccion> transacciones;
        
        // Si hay filtros, usar búsqueda filtrada
        if (fechaDesde != null || fechaHasta != null || tipo != null || 
            montoMinimo != null || montoMaximo != null) {
            transacciones = transaccionService.obtenerTransaccionesConFiltros(
                fechaDesde, fechaHasta, tipo, montoMinimo, montoMaximo, pageable);
        } else {
            // Sin filtros, obtener todas con paginación
            transacciones = transaccionService.obtenerTransaccionesPaginadas(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", transacciones.getContent());
        response.put("pageable", Map.of(
            "page", transacciones.getNumber(),
            "size", transacciones.getSize(),
            "totalElements", transacciones.getTotalElements(),
            "totalPages", transacciones.getTotalPages(),
            "first", transacciones.isFirst(),
            "last", transacciones.isLast()
        ));
        response.put("filters_applied", Map.of(
            "fechaDesde", fechaDesde,
            "fechaHasta", fechaHasta,
            "tipo", tipo,
            "montoMinimo", montoMinimo,
            "montoMaximo", montoMaximo
        ));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener anomalías detectadas", 
               description = "Lista de transacciones marcadas como anomalías por el sistema batch")
    @GetMapping("/anomalias")
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> obtenerAnomalias() {
        List<Transaccion> anomalias = transaccionService.obtenerAnomalias();
        
        Map<String, Object> response = new HashMap<>();
        response.put("anomalias", anomalias);
        response.put("total_anomalias", anomalias.size());
        response.put("clasificacion", clasificarAnomalias(anomalias));
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener estadísticas de transacciones", 
               description = "Resumen estadístico completo de transacciones para dashboard web")
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        TransaccionService.TransaccionResumen resumen = transaccionService.obtenerResumenTransacciones();
        
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("resumen_general", resumen);
        estadisticas.put("anomalias_detectadas", transaccionService.obtenerAnomalias().size());
        estadisticas.put("timestamp", System.currentTimeMillis());
        estadisticas.put("periodo", "Desde el inicio de registros");
        
        return ResponseEntity.ok(estadisticas);
    }

    @Operation(summary = "Exportar transacciones", 
               description = "Exportar transacciones en formato JSON para reportes")
    @GetMapping("/export")
    @PreAuthorize("hasRole('WEB') and hasAuthority('EXPORT_REPORTS')")
    public ResponseEntity<Map<String, Object>> exportarTransacciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "1000") int limit) {

        // Usar un límite máximo para exportación
        Pageable pageable = PageRequest.of(0, Math.min(limit, 1000));
        
        Page<Transaccion> transacciones = transaccionService.obtenerTransaccionesConFiltros(
            fechaDesde, fechaHasta, null, null, null, pageable);

        Map<String, Object> export = new HashMap<>();
        export.put("export_timestamp", System.currentTimeMillis());
        export.put("total_records", transacciones.getTotalElements());
        export.put("exported_records", transacciones.getContent().size());
        export.put("filters", Map.of(
            "fechaDesde", fechaDesde,
            "fechaHasta", fechaHasta,
            "limit", limit
        ));
        export.put("data", transacciones.getContent());

        return ResponseEntity.ok(export);
    }

    @Operation(summary = "Buscar transacciones", 
               description = "Búsqueda avanzada de transacciones con múltiples criterios")
    @PostMapping("/search")
    @PreAuthorize("hasRole('WEB')")
    public ResponseEntity<Map<String, Object>> buscarTransacciones(
            @RequestBody SearchRequest searchRequest) {

        Pageable pageable = PageRequest.of(
            searchRequest.getPage(), 
            Math.min(searchRequest.getSize(), 100)
        );

        Page<Transaccion> resultados = transaccionService.obtenerTransaccionesConFiltros(
            searchRequest.getFechaDesde(),
            searchRequest.getFechaHasta(),
            searchRequest.getTipo(),
            searchRequest.getMontoMinimo(),
            searchRequest.getMontoMaximo(),
            pageable
        );

        Map<String, Object> response = new HashMap<>();
        response.put("results", resultados.getContent());
        response.put("search_criteria", searchRequest);
        response.put("pagination", Map.of(
            "current_page", resultados.getNumber(),
            "total_pages", resultados.getTotalPages(),
            "total_elements", resultados.getTotalElements(),
            "has_next", resultados.hasNext(),
            "has_previous", resultados.hasPrevious()
        ));

        return ResponseEntity.ok(response);
    }

    // Métodos auxiliares

    private Map<String, Object> clasificarAnomalias(List<Transaccion> anomalias) {
        Map<String, Object> clasificacion = new HashMap<>();
        
        long montosAltos = anomalias.stream()
            .filter(t -> t.getMonto().compareTo(new BigDecimal("10000")) > 0)
            .count();
            
        long montosNegativos = anomalias.stream()
            .filter(t -> t.getMonto().compareTo(BigDecimal.ZERO) < 0)
            .count();
            
        long montosZero = anomalias.stream()
            .filter(t -> t.getMonto().compareTo(BigDecimal.ZERO) == 0)
            .count();

        clasificacion.put("montos_altos", montosAltos);
        clasificacion.put("montos_negativos", montosNegativos);
        clasificacion.put("montos_zero", montosZero);
        clasificacion.put("otros", anomalias.size() - montosAltos - montosNegativos - montosZero);

        return clasificacion;
    }

    // DTOs para requests complejos
    public static class SearchRequest {
        private int page = 0;
        private int size = 50;
        private LocalDate fechaDesde;
        private LocalDate fechaHasta;
        private String tipo;
        private BigDecimal montoMinimo;
        private BigDecimal montoMaximo;

        // Getters y Setters
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public LocalDate getFechaDesde() { return fechaDesde; }
        public void setFechaDesde(LocalDate fechaDesde) { this.fechaDesde = fechaDesde; }
        
        public LocalDate getFechaHasta() { return fechaHasta; }
        public void setFechaHasta(LocalDate fechaHasta) { this.fechaHasta = fechaHasta; }
        
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        
        public BigDecimal getMontoMinimo() { return montoMinimo; }
        public void setMontoMinimo(BigDecimal montoMinimo) { this.montoMinimo = montoMinimo; }
        
        public BigDecimal getMontoMaximo() { return montoMaximo; }
        public void setMontoMaximo(BigDecimal montoMaximo) { this.montoMaximo = montoMaximo; }
    }
}
