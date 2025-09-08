package com.duoc.batch_demo.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.duoc.batch_demo.model.Cuenta;

/**
 * Servicio central para el manejo de cuentas bancarias.
 * Utilizado por todos los BFFs para acceder a datos de cuentas.
 */
@Service
public class CuentaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Cuenta mapearCuenta(ResultSet rs, int rowNum) throws SQLException {
        Cuenta cuenta = new Cuenta();
        cuenta.setCuentaId(rs.getLong("cuenta_id"));
        cuenta.setNombre(rs.getString("nombre"));
        cuenta.setSaldo(rs.getBigDecimal("saldo"));
        cuenta.setEdad(rs.getInt("edad"));
        cuenta.setTipo(rs.getString("tipo"));
        return cuenta;
    }

    /**
     * Obtiene todas las cuentas con paginación (para BFF Web)
     */
    public Page<Cuenta> obtenerCuentasPaginadas(Pageable pageable) {
        String countQuery = "SELECT COUNT(*) FROM intereses_calculados";
        Long total = jdbcTemplate.queryForObject(countQuery, Long.class);

        String query = "SELECT * FROM intereses_calculados ORDER BY cuenta_id LIMIT ? OFFSET ?";
        List<Cuenta> cuentas = jdbcTemplate.query(query, this::mapearCuenta,
            pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(cuentas, pageable, total != null ? total : 0);
    }

    /**
     * Obtiene una cuenta específica por ID
     */
    public Optional<Cuenta> obtenerCuentaPorId(Long cuentaId) {
        String query = "SELECT * FROM intereses_calculados WHERE cuenta_id = ?";
        try {
            Cuenta cuenta = jdbcTemplate.queryForObject(query, this::mapearCuenta, cuentaId);
            return Optional.ofNullable(cuenta);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Obtiene saldo de una cuenta (para ATM y móvil)
     */
    public Optional<BigDecimal> obtenerSaldo(Long cuentaId) {
        String query = "SELECT saldo FROM intereses_calculados WHERE cuenta_id = ?";
        try {
            BigDecimal saldo = jdbcTemplate.queryForObject(query, BigDecimal.class, cuentaId);
            return Optional.ofNullable(saldo);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Obtiene resumen de cuentas para dashboard
     */
    public CuentaResumen obtenerResumenCuentas() {
        String query = """
            SELECT 
                COUNT(*) as total_cuentas,
                SUM(saldo) as saldo_total,
                AVG(saldo) as saldo_promedio,
                COUNT(CASE WHEN tipo = 'ahorro' THEN 1 END) as cuentas_ahorro,
                COUNT(CASE WHEN tipo = 'prestamo' THEN 1 END) as cuentas_prestamo,
                COUNT(CASE WHEN tipo = 'hipoteca' THEN 1 END) as cuentas_hipoteca
            FROM intereses_calculados
        """;
        
        return jdbcTemplate.queryForObject(query, (rs, rowNum) -> {
            CuentaResumen resumen = new CuentaResumen();
            resumen.setTotalCuentas(rs.getLong("total_cuentas"));
            resumen.setSaldoTotal(rs.getBigDecimal("saldo_total"));
            resumen.setSaldoPromedio(rs.getBigDecimal("saldo_promedio"));
            resumen.setCuentasAhorro(rs.getInt("cuentas_ahorro"));
            resumen.setCuentasPrestamo(rs.getInt("cuentas_prestamo"));
            resumen.setCuentasHipoteca(rs.getInt("cuentas_hipoteca"));
            return resumen;
        });
    }

    /**
     * Obtiene cuentas por tipo
     */
    public List<Cuenta> obtenerCuentasPorTipo(String tipo) {
        String query = "SELECT * FROM intereses_calculados WHERE tipo = ? ORDER BY saldo DESC";
        return jdbcTemplate.query(query, this::mapearCuenta, tipo);
    }

    /**
     * Verifica si una cuenta existe y está activa (para ATM)
     */
    public boolean validarCuentaActiva(Long cuentaId) {
        String query = "SELECT COUNT(*) FROM intereses_calculados WHERE cuenta_id = ? AND saldo >= 0";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, cuentaId);
        return count != null && count > 0;
    }

    /**
     * Actualiza saldo de cuenta (para operaciones ATM)
     */
    public boolean actualizarSaldo(Long cuentaId, BigDecimal nuevoSaldo) {
        String query = "UPDATE intereses_calculados SET saldo = ? WHERE cuenta_id = ?";
        int rowsAffected = jdbcTemplate.update(query, nuevoSaldo, cuentaId);
        return rowsAffected > 0;
    }

    /**
     * Clase interna para resúmenes de cuentas
     */
    public static class CuentaResumen {
        private Long totalCuentas;
        private BigDecimal saldoTotal;
        private BigDecimal saldoPromedio;
        private Integer cuentasAhorro;
        private Integer cuentasPrestamo;
        private Integer cuentasHipoteca;

        // Getters y Setters
        public Long getTotalCuentas() { return totalCuentas; }
        public void setTotalCuentas(Long totalCuentas) { this.totalCuentas = totalCuentas; }
        
        public BigDecimal getSaldoTotal() { return saldoTotal; }
        public void setSaldoTotal(BigDecimal saldoTotal) { this.saldoTotal = saldoTotal; }
        
        public BigDecimal getSaldoPromedio() { return saldoPromedio; }
        public void setSaldoPromedio(BigDecimal saldoPromedio) { this.saldoPromedio = saldoPromedio; }
        
        public Integer getCuentasAhorro() { return cuentasAhorro; }
        public void setCuentasAhorro(Integer cuentasAhorro) { this.cuentasAhorro = cuentasAhorro; }
        
        public Integer getCuentasPrestamo() { return cuentasPrestamo; }
        public void setCuentasPrestamo(Integer cuentasPrestamo) { this.cuentasPrestamo = cuentasPrestamo; }
        
        public Integer getCuentasHipoteca() { return cuentasHipoteca; }
        public void setCuentasHipoteca(Integer cuentasHipoteca) { this.cuentasHipoteca = cuentasHipoteca; }
    }
}
