package com.duoc.batch_demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.duoc.batch_demo.model.Transaccion;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio central para el manejo de transacciones.
 * Utilizado por todos los BFFs para acceder a datos de transacciones.
 */
@Service
public class TransaccionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Transaccion> transaccionRowMapper = new RowMapper<Transaccion>() {
        @Override
        public Transaccion mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transaccion transaccion = new Transaccion();
            transaccion.setId(rs.getLong("id"));
            transaccion.setFecha(rs.getDate("fecha").toLocalDate());
            transaccion.setMonto(rs.getBigDecimal("monto"));
            transaccion.setTipo(rs.getString("tipo"));
            
            // Campos opcionales para anomalías
            try {
                transaccion.setEsAnomalia(rs.getBoolean("es_anomalia"));
                transaccion.setMotivoAnomalia(rs.getString("motivo_anomalia"));
            } catch (SQLException e) {
                // Si no existen estos campos, los dejamos en null
                transaccion.setEsAnomalia(false);
            }
            
            return transaccion;
        }
    };

    /**
     * Obtiene todas las transacciones con paginación (para BFF Web)
     */
    public Page<Transaccion> obtenerTransaccionesPaginadas(Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM transacciones";
        Long total = jdbcTemplate.queryForObject(countQuery, Long.class);

        String query = "SELECT * FROM transacciones ORDER BY fecha DESC LIMIT ? OFFSET ?";
        List<Transaccion> transacciones = jdbcTemplate.query(
            query, 
            transaccionRowMapper,
            pageable.getPageSize(),
            pageable.getOffset()
        );

        return new PageImpl<>(transacciones, pageable, total != null ? total : 0);
    }

    /**
     * Obtiene transacciones recientes limitadas (para BFF Móvil)
     */
    public List<Transaccion> obtenerTransaccionesRecientes(int limit) {
        String query = "SELECT * FROM transacciones ORDER BY fecha DESC LIMIT ?";
        return jdbcTemplate.query(query, transaccionRowMapper, limit);
    }

    /**
     * Obtiene transacciones por cuenta (para ATM)
     */
    public List<Transaccion> obtenerTransaccionesPorCuenta(Long cuentaId, int limit) {
        // Nota: Esta consulta asume que existe una relación cuenta-transacción
        // Si no existe, se puede usar fecha y monto para correlacionar
        String query = """
            SELECT t.* FROM transacciones t 
            WHERE t.fecha >= CURDATE() - INTERVAL 30 DAY 
            ORDER BY t.fecha DESC 
            LIMIT ?
        """;
        return jdbcTemplate.query(query, transaccionRowMapper, limit);
    }

    /**
     * Obtiene transacciones con filtros avanzados (para BFF Web)
     */
    public Page<Transaccion> obtenerTransaccionesConFiltros(
            LocalDate fechaDesde, 
            LocalDate fechaHasta, 
            String tipo, 
            BigDecimal montoMinimo,
            BigDecimal montoMaximo,
            Pageable pageable) {
        
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        
        if (fechaDesde != null) {
            whereClause.append(" AND fecha >= '").append(fechaDesde).append("'");
        }
        if (fechaHasta != null) {
            whereClause.append(" AND fecha <= '").append(fechaHasta).append("'");
        }
        if (tipo != null && !tipo.isEmpty()) {
            whereClause.append(" AND tipo = '").append(tipo).append("'");
        }
        if (montoMinimo != null) {
            whereClause.append(" AND monto >= ").append(montoMinimo);
        }
        if (montoMaximo != null) {
            whereClause.append(" AND monto <= ").append(montoMaximo);
        }

        String countQuery = "SELECT COUNT(*) FROM transacciones" + whereClause;
        Long total = jdbcTemplate.queryForObject(countQuery, Long.class);

        String query = "SELECT * FROM transacciones" + whereClause + 
                      " ORDER BY fecha DESC LIMIT ? OFFSET ?";
        List<Transaccion> transacciones = jdbcTemplate.query(
            query, 
            transaccionRowMapper,
            pageable.getPageSize(),
            pageable.getOffset()
        );

        return new PageImpl<>(transacciones, pageable, total != null ? total : 0);
    }

    /**
     * Obtiene anomalías de transacciones (para BFF Web)
     */
    public List<Transaccion> obtenerAnomalias() {
        String query = """
            SELECT t.*, a.motivo_anomalia, true as es_anomalia
            FROM transacciones t 
            INNER JOIN anomalias_transacciones a ON t.id = a.transaccion_id
            ORDER BY t.fecha DESC
        """;
        return jdbcTemplate.query(query, transaccionRowMapper);
    }

    /**
     * Obtiene resumen de transacciones (para móvil y dashboard)
     */
    public TransaccionResumen obtenerResumenTransacciones() {
        String query = """
            SELECT 
                COUNT(*) as total_transacciones,
                SUM(CASE WHEN tipo = 'credito' THEN monto ELSE 0 END) as total_creditos,
                SUM(CASE WHEN tipo = 'debito' THEN monto ELSE 0 END) as total_debitos,
                COUNT(CASE WHEN fecha = CURDATE() THEN 1 END) as transacciones_hoy
            FROM transacciones
        """;
        
        return jdbcTemplate.queryForObject(query, (rs, rowNum) -> {
            TransaccionResumen resumen = new TransaccionResumen();
            resumen.setTotalTransacciones(rs.getLong("total_transacciones"));
            resumen.setTotalCreditos(rs.getBigDecimal("total_creditos"));
            resumen.setTotalDebitos(rs.getBigDecimal("total_debitos"));
            resumen.setTransaccionesHoy(rs.getInt("transacciones_hoy"));
            return resumen;
        });
    }

    /**
     * Clase interna para resúmenes
     */
    public static class TransaccionResumen {
        private Long totalTransacciones;
        private BigDecimal totalCreditos;
        private BigDecimal totalDebitos;
        private Integer transaccionesHoy;

        // Getters y Setters
        public Long getTotalTransacciones() { return totalTransacciones; }
        public void setTotalTransacciones(Long totalTransacciones) { this.totalTransacciones = totalTransacciones; }
        
        public BigDecimal getTotalCreditos() { return totalCreditos; }
        public void setTotalCreditos(BigDecimal totalCreditos) { this.totalCreditos = totalCreditos; }
        
        public BigDecimal getTotalDebitos() { return totalDebitos; }
        public void setTotalDebitos(BigDecimal totalDebitos) { this.totalDebitos = totalDebitos; }
        
        public Integer getTransaccionesHoy() { return transaccionesHoy; }
        public void setTransaccionesHoy(Integer transaccionesHoy) { this.transaccionesHoy = transaccionesHoy; }
    }
}
