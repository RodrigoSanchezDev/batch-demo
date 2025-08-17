package com.duoc.batch_demo.model;

import java.time.LocalDateTime;

public class AnomaliaTransaccion {
    
    private Long id;
    private Long transaccionId;
    private String tipoAnomalia;
    private String descripcion;
    private LocalDateTime fechaDeteccion;
    private String severidad;

    // Constructor por defecto
    public AnomaliaTransaccion() {
        this.fechaDeteccion = LocalDateTime.now();
        this.severidad = "MEDIA";
    }

    // Constructor con parámetros principales
    public AnomaliaTransaccion(Long transaccionId, String tipoAnomalia, String descripcion) {
        this();
        this.transaccionId = transaccionId;
        this.tipoAnomalia = tipoAnomalia;
        this.descripcion = descripcion;
    }

    // Constructor completo
    public AnomaliaTransaccion(Long transaccionId, String tipoAnomalia, String descripcion, String severidad) {
        this(transaccionId, tipoAnomalia, descripcion);
        this.severidad = severidad;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTransaccionId() { return transaccionId; }
    public void setTransaccionId(Long transaccionId) { this.transaccionId = transaccionId; }

    public String getTipoAnomalia() { return tipoAnomalia; }
    public void setTipoAnomalia(String tipoAnomalia) { this.tipoAnomalia = tipoAnomalia; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaDeteccion() { return fechaDeteccion; }
    public void setFechaDeteccion(LocalDateTime fechaDeteccion) { this.fechaDeteccion = fechaDeteccion; }

    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }

    @Override
    public String toString() {
        return "AnomaliaTransaccion{" +
                "transaccionId=" + transaccionId +
                ", tipoAnomalia='" + tipoAnomalia + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", severidad='" + severidad + '\'' +
                '}';
    }
}
