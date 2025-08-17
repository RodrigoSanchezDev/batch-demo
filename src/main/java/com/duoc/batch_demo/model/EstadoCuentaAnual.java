package com.duoc.batch_demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EstadoCuentaAnual {
    
    private Long id;
    private Long cuentaId;
    private String nombre;
    private BigDecimal totalDepositos;
    private BigDecimal totalRetiros;
    private BigDecimal saldoInicial;
    private BigDecimal saldoFinal;
    private Integer totalTransacciones;
    private BigDecimal interesesGanados;
    private Integer año;
    private LocalDateTime fechaGeneracion;

    // Constructor por defecto
    public EstadoCuentaAnual() {
        this.totalDepositos = BigDecimal.ZERO;
        this.totalRetiros = BigDecimal.ZERO;
        this.saldoInicial = BigDecimal.ZERO;
        this.saldoFinal = BigDecimal.ZERO;
        this.totalTransacciones = 0;
        this.interesesGanados = BigDecimal.ZERO;
        this.fechaGeneracion = LocalDateTime.now();
        this.año = fechaGeneracion.getYear();
    }

    // Constructor con parámetros principales
    public EstadoCuentaAnual(Long cuentaId, String nombre) {
        this();
        this.cuentaId = cuentaId;
        this.nombre = nombre;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getTotalDepositos() { return totalDepositos; }
    public void setTotalDepositos(BigDecimal totalDepositos) { this.totalDepositos = totalDepositos; }

    public BigDecimal getTotalRetiros() { return totalRetiros; }
    public void setTotalRetiros(BigDecimal totalRetiros) { this.totalRetiros = totalRetiros; }

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }

    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }

    public Integer getTotalTransacciones() { return totalTransacciones; }
    public void setTotalTransacciones(Integer totalTransacciones) { this.totalTransacciones = totalTransacciones; }

    public BigDecimal getInteresesGanados() { return interesesGanados; }
    public void setInteresesGanados(BigDecimal interesesGanados) { this.interesesGanados = interesesGanados; }

    public Integer getAño() { return año; }
    public void setAño(Integer año) { this.año = año; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    @Override
    public String toString() {
        return "EstadoCuentaAnual{" +
                "cuentaId=" + cuentaId +
                ", nombre='" + nombre + '\'' +
                ", totalDepositos=" + totalDepositos +
                ", totalRetiros=" + totalRetiros +
                ", saldoFinal=" + saldoFinal +
                ", totalTransacciones=" + totalTransacciones +
                ", año=" + año +
                '}';
    }
}
