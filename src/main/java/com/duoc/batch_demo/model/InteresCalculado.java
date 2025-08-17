package com.duoc.batch_demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InteresCalculado {
    
    private Long id;
    private Long cuentaId;
    private BigDecimal saldoAnterior;
    private BigDecimal tasaInteres;
    private BigDecimal interesCalculado;
    private BigDecimal saldoNuevo;
    private LocalDateTime fechaCalculo;
    private String tipoCuenta;

    // Constructor por defecto
    public InteresCalculado() {
        this.fechaCalculo = LocalDateTime.now();
    }

    // Constructor con parámetros principales
    public InteresCalculado(Long cuentaId, BigDecimal saldoAnterior, BigDecimal tasaInteres, 
                           BigDecimal interesCalculado, BigDecimal saldoNuevo, String tipoCuenta) {
        this();
        this.cuentaId = cuentaId;
        this.saldoAnterior = saldoAnterior;
        this.tasaInteres = tasaInteres;
        this.interesCalculado = interesCalculado;
        this.saldoNuevo = saldoNuevo;
        this.tipoCuenta = tipoCuenta;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }

    public BigDecimal getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(BigDecimal saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public BigDecimal getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(BigDecimal tasaInteres) { this.tasaInteres = tasaInteres; }

    public BigDecimal getInteresCalculado() { return interesCalculado; }
    public void setInteresCalculado(BigDecimal interesCalculado) { this.interesCalculado = interesCalculado; }

    public BigDecimal getSaldoNuevo() { return saldoNuevo; }
    public void setSaldoNuevo(BigDecimal saldoNuevo) { this.saldoNuevo = saldoNuevo; }

    public LocalDateTime getFechaCalculo() { return fechaCalculo; }
    public void setFechaCalculo(LocalDateTime fechaCalculo) { this.fechaCalculo = fechaCalculo; }

    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }

    @Override
    public String toString() {
        return "InteresCalculado{" +
                "cuentaId=" + cuentaId +
                ", saldoAnterior=" + saldoAnterior +
                ", tasaInteres=" + tasaInteres +
                ", interesCalculado=" + interesCalculado +
                ", saldoNuevo=" + saldoNuevo +
                ", tipoCuenta='" + tipoCuenta + '\'' +
                '}';
    }
}
