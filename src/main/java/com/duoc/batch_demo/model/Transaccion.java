package com.duoc.batch_demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Transaccion {
    private Long id;
    private LocalDate fecha;
    private BigDecimal monto;
    private String tipo;
    private LocalDateTime fechaProcesamiento;
    private Boolean esAnomalia;
    private String motivoAnomalia;

    // Constructor vacío
    public Transaccion() {}

    // Constructor con parámetros básicos
    public Transaccion(Long id, LocalDate fecha, BigDecimal monto, String tipo) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.esAnomalia = false;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Boolean getEsAnomalia() {
        return esAnomalia;
    }

    public void setEsAnomalia(Boolean esAnomalia) {
        this.esAnomalia = esAnomalia;
    }

    public String getMotivoAnomalia() {
        return motivoAnomalia;
    }

    public void setMotivoAnomalia(String motivoAnomalia) {
        this.motivoAnomalia = motivoAnomalia;
    }

    @Override
    public String toString() {
        return "Transaccion{" +
                "id=" + id +
                ", fecha=" + fecha +
                ", monto=" + monto +
                ", tipo='" + tipo + '\'' +
                ", esAnomalia=" + esAnomalia +
                ", motivoAnomalia='" + motivoAnomalia + '\'' +
                '}';
    }
}
