package com.duoc.batch_demo.validator;

import java.math.BigDecimal;

import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.stereotype.Component;

import com.duoc.batch_demo.model.Cuenta;

/**
 * 🔍 VALIDADOR PERSONALIZADO PARA CUENTAS BANCARIAS
 * 
 * Implementa validaciones específicas para cuentas bancarias
 * con políticas de tolerancia a fallos personalizadas.
 * 
 * @author Rodrigo Sanchez
 * @version 1.0
 * @since 2025
 */
@Component
public class CuentaValidator implements Validator<Cuenta> {

    private static final BigDecimal SALDO_MINIMO_PERMITIDO = new BigDecimal("-50000.00"); // Sobregiro máximo
    private static final BigDecimal SALDO_MAXIMO_PERMITIDO = new BigDecimal("10000000.00"); // 10 millones
    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = 120;

    /**
     * 🔍 Valida una cuenta bancaria aplicando reglas de negocio estrictas
     * 
     * @param cuenta La cuenta a validar
     * @throws ValidationException Si la cuenta no cumple las reglas
     */
    @Override
    public void validate(Cuenta cuenta) throws ValidationException {
        StringBuilder errores = new StringBuilder();
        
        // 1. VALIDACIÓN DE ID DE CUENTA
        if (cuenta.getCuentaId() == null || cuenta.getCuentaId() <= 0) {
            errores.append("ID de cuenta debe ser un número positivo; ");
        }
        
        // 2. VALIDACIÓN DE NOMBRE
        if (cuenta.getNombre() == null || cuenta.getNombre().trim().isEmpty()) {
            errores.append("Nombre del titular no puede estar vacío; ");
        } else {
            String nombre = cuenta.getNombre().trim();
            
            // Nombre muy corto
            if (nombre.length() < 2) {
                errores.append("Nombre muy corto (mínimo 2 caracteres); ");
            }
            
            // Nombre muy largo
            if (nombre.length() > 100) {
                errores.append("Nombre muy largo (máximo 100 caracteres); ");
            }
            
            // Solo letras, espacios y algunos caracteres especiales
            if (!nombre.matches("^[a-zA-ZÀ-ÿ\\u00f1\\u00d1\\s'.-]+$")) {
                errores.append("Nombre contiene caracteres no válidos; ");
            }
        }
        
        // 3. VALIDACIÓN DE SALDO
        if (cuenta.getSaldo() == null) {
            errores.append("Saldo no puede ser nulo; ");
        } else {
            // Saldo muy negativo (sobregiro excesivo)
            if (cuenta.getSaldo().compareTo(SALDO_MINIMO_PERMITIDO) < 0) {
                errores.append("Sobregiro excede el límite permitido (").append(SALDO_MINIMO_PERMITIDO).append("); ");
            }
            
            // Saldo excesivamente alto
            if (cuenta.getSaldo().compareTo(SALDO_MAXIMO_PERMITIDO) > 0) {
                errores.append("Saldo excede el límite máximo (").append(SALDO_MAXIMO_PERMITIDO).append("); ");
            }
        }
        
        // 4. VALIDACIÓN DE EDAD
        if (cuenta.getEdad() == null) {
            errores.append("Edad no puede ser nula; ");
        } else {
            if (cuenta.getEdad() < EDAD_MINIMA) {
                errores.append("Edad debe ser mayor o igual a ").append(EDAD_MINIMA).append(" años; ");
            }
            
            if (cuenta.getEdad() > EDAD_MAXIMA) {
                errores.append("Edad no puede ser mayor a ").append(EDAD_MAXIMA).append(" años; ");
            }
        }
        
        // 5. VALIDACIÓN DE TIPO DE CUENTA
        if (cuenta.getTipo() == null || cuenta.getTipo().trim().isEmpty()) {
            errores.append("Tipo de cuenta no puede estar vacío; ");
        } else {
            String tipo = cuenta.getTipo().toUpperCase().trim();
            String[] tiposValidos = {"AHORRO", "CORRIENTE", "PRESTAMO", "HIPOTECA", "INVERSION"};
            boolean tipoValido = false;
            
            for (String tipoPermitido : tiposValidos) {
                if (tipoPermitido.equals(tipo)) {
                    tipoValido = true;
                    break;
                }
            }
            
            if (!tipoValido) {
                errores.append("Tipo de cuenta no válido: ").append(tipo).append(" (permitidos: AHORRO, CORRIENTE, PRESTAMO, HIPOTECA, INVERSION); ");
            }
        }
        
        // 6. VALIDACIONES DE CONSISTENCIA LÓGICA
        if (cuenta.getTipo() != null && cuenta.getSaldo() != null) {
            String tipo = cuenta.getTipo().toUpperCase().trim();
            
            // Validaciones específicas por tipo de cuenta
            switch (tipo) {
                case "AHORRO":
                    // Las cuentas de ahorro no pueden tener saldo negativo
                    if (cuenta.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
                        errores.append("Cuenta de ahorro no puede tener saldo negativo; ");
                    }
                    break;
                    
                case "PRESTAMO":
                case "HIPOTECA":
                    // Los préstamos e hipotecas normalmente tienen saldo negativo (deuda)
                    if (cuenta.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
                        errores.append("Cuenta de préstamo/hipoteca con saldo positivo es inusual; ");
                    }
                    break;
                    
                case "CORRIENTE":
                    // Las cuentas corrientes pueden tener sobregiro limitado
                    BigDecimal sobregiroMaximo = new BigDecimal("-5000.00");
                    if (cuenta.getSaldo().compareTo(sobregiroMaximo) < 0) {
                        errores.append("Sobregiro en cuenta corriente excede límite (-5000); ");
                    }
                    break;
            }
        }
        
        // 7. VALIDACIÓN DE FECHA DE ACTUALIZACIÓN
        if (cuenta.getFechaActualizacion() != null) {
            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
            if (cuenta.getFechaActualizacion().isAfter(ahora)) {
                errores.append("Fecha de actualización no puede ser futura; ");
            }
        }
        
        // Si hay errores, lanzar excepción de validación
        if (errores.length() > 0) {
            String mensajeError = "Errores de validación en cuenta ID " + 
                                cuenta.getCuentaId() + ": " + errores.toString();
            
            System.out.println("❌ VALIDACIÓN DE CUENTA FALLIDA: " + mensajeError);
            
            // Crear excepción personalizada
            ValidationException exception = new ValidationException(mensajeError);
            throw exception;
        }
        
        // Si llegamos aquí, la validación fue exitosa
        System.out.println("✅ CUENTA VÁLIDA: ID " + cuenta.getCuentaId() + 
                         ", Titular: " + cuenta.getNombre() + 
                         ", Tipo: " + cuenta.getTipo() + 
                         ", Saldo: " + cuenta.getSaldo());
    }
    
    /**
     * 📊 Obtiene el nivel de riesgo de una cuenta
     * 
     * @param cuenta La cuenta a evaluar
     * @return String con el nivel de riesgo
     */
    public String evaluarNivelRiesgo(Cuenta cuenta) {
        if (cuenta == null || cuenta.getSaldo() == null) {
            return "DESCONOCIDO - Datos insuficientes";
        }
        
        BigDecimal saldo = cuenta.getSaldo();
        String tipo = cuenta.getTipo() != null ? cuenta.getTipo().toUpperCase() : "DESCONOCIDO";
        
        // Evaluar riesgo basado en saldo y tipo
        if (saldo.compareTo(new BigDecimal("-10000")) < 0) {
            return "ALTO - Sobregiro significativo";
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            return "MEDIO - Saldo negativo";
        } else if (saldo.compareTo(new BigDecimal("1000000")) > 0) {
            return "MEDIO - Saldo muy alto requiere monitoreo";
        } else {
            return "BAJO - Cuenta normal";
        }
    }
}
