package com.duoc.batch_demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CuentaAnual {
    private Long cuentaId;
    private LocalDate fecha;
    private String transaccion;
    private BigDecimal monto;
    private String descripcion;
    private LocalDateTime fechaProcesamiento;

    // Constructor vacío
    public CuentaAnual() {}

    // Constructor con parámetros
    public CuentaAnual(Long cuentaId, LocalDate fecha, String transaccion, BigDecimal monto, String descripcion) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTransaccion() {
        return transaccion;
    }

    public void setTransaccion(String transaccion) {
        this.transaccion = transaccion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    @Override
    public String toString() {
        return "CuentaAnual{" +
                "cuentaId=" + cuentaId +
                ", fecha=" + fecha +
                ", transaccion='" + transaccion + '\'' +
                ", monto=" + monto +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}
