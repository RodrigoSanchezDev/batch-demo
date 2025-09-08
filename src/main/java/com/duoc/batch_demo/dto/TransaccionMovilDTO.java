package com.duoc.batch_demo.dto;

import java.math.BigDecimal;

/**
 * DTO ligero para transacciones móviles.
 * Contiene solo los campos esenciales para reducir ancho de banda.
 */
public class TransaccionMovilDTO {
    private Long id;
    private String fecha; // Formato simplificado
    private String monto; // Ya formateado para mostrar
    private String tipo;
    private Boolean anomalia;

    // Constructores
    public TransaccionMovilDTO() {}

    public TransaccionMovilDTO(Long id, String fecha, BigDecimal monto, String tipo, Boolean anomalia) {
        this.id = id;
        this.fecha = fecha;
        this.monto = formatearMonto(monto);
        this.tipo = tipo;
        this.anomalia = anomalia != null && anomalia;
    }

    // Método estático para formatear montos de manera consistente
    private String formatearMonto(BigDecimal monto) {
        if (monto == null) return "$0";
        return String.format("$%,.0f", monto); // Sin decimales para móvil
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Boolean getAnomalia() { return anomalia; }
    public void setAnomalia(Boolean anomalia) { this.anomalia = anomalia; }
}
