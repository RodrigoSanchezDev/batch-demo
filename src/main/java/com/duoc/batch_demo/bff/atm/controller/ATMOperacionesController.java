package com.duoc.batch_demo.bff.atm.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.batch_demo.service.CuentaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador BFF ATM para operaciones bancarias críticas.
 * Máxima seguridad y validaciones estrictas para cajeros automáticos.
 */
@RestController
@RequestMapping("/api/atm/operaciones")
@Tag(name = "ATM Operations", description = "Operaciones bancarias ultra-seguras para ATMs")
public class ATMOperacionesController {

    @Autowired
    private CuentaService cuentaService;

    @Operation(summary = "Consultar saldo seguro", 
               description = "Consulta de saldo con validaciones de seguridad ATM")
    @GetMapping("/saldo/{cuentaId}")
    @PreAuthorize("hasRole('ATM')")
    public ResponseEntity<Map<String, Object>> consultarSaldo(
            @Parameter(description = "ID de la cuenta") 
            @PathVariable Long cuentaId,
            
            @RequestHeader("ATM-ID") String atmId,
            @RequestHeader("Session-ID") String sessionId) {

        // Validaciones de seguridad
        if (!isValidATMOperation(atmId, sessionId)) {
            return ResponseEntity.status(403).body(Map.of("error", "invalid_atm_session"));
        }

        // Verificar que la cuenta existe y está activa
        if (!cuentaService.validarCuentaActiva(cuentaId)) {
            logSecurityEvent(atmId, "invalid_account_access", cuentaId.toString());
            return ResponseEntity.status(404).body(Map.of("error", "account_not_found"));
        }

        Optional<BigDecimal> saldoOpt = cuentaService.obtenerSaldo(cuentaId);
        
        if (saldoOpt.isEmpty()) {
            return ResponseEntity.status(500).body(Map.of("error", "saldo_unavailable"));
        }

        // Log de auditoría para consulta de saldo
        logATMOperation(atmId, sessionId, "balance_inquiry", cuentaId.toString());

        Map<String, Object> response = new HashMap<>();
        response.put("account_id", cuentaId);
        response.put("balance", formatearSaldoATM(saldoOpt.get()));
        response.put("currency", "CLP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("atm_id", atmId);
        response.put("operation_id", generarOperationId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validar retiro", 
               description = "Pre-validación de retiro antes de dispensar efectivo")
    @PostMapping("/retiro/validate")
    @PreAuthorize("hasRole('ATM')")
    public ResponseEntity<Map<String, Object>> validarRetiro(
            @RequestBody RetiroValidationRequest request,
            @RequestHeader("ATM-ID") String atmId,
            @RequestHeader("Session-ID") String sessionId) {

        // Validaciones de seguridad y negocio
        Map<String, String> validationResult = validarOperacionRetiro(request, atmId, sessionId);
        
        if (!validationResult.isEmpty()) {
            logSecurityEvent(atmId, "invalid_withdrawal_attempt", request.toString());
            return ResponseEntity.status(400).body(Map.of("errors", validationResult));
        }

        // Verificar saldo disponible
        Optional<BigDecimal> saldoOpt = cuentaService.obtenerSaldo(request.getCuentaId());
        
        if (saldoOpt.isEmpty() || saldoOpt.get().compareTo(request.getMonto()) < 0) {
            logATMOperation(atmId, sessionId, "insufficient_funds", request.getCuentaId().toString());
            return ResponseEntity.status(400).body(Map.of("error", "insufficient_funds"));
        }

        // Generar token de autorización temporal
        String authToken = UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("validation_successful", true);
        response.put("authorization_token", authToken);
        response.put("amount_authorized", request.getMonto());
        response.put("account_id", request.getCuentaId());
        response.put("expires_in", 60); // 60 segundos para ejecutar
        response.put("new_balance_preview", saldoOpt.get().subtract(request.getMonto()));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Ejecutar retiro", 
               description = "Ejecutar retiro autorizado y actualizar saldo")
    @PostMapping("/retiro/execute")
    @PreAuthorize("hasRole('ATM')")
    public ResponseEntity<Map<String, Object>> ejecutarRetiro(
            @RequestBody RetiroExecutionRequest request,
            @RequestHeader("ATM-ID") String atmId,
            @RequestHeader("Session-ID") String sessionId) {

        // Validar token de autorización
        if (!isValidAuthToken(request.getAuthToken())) {
            logSecurityEvent(atmId, "invalid_auth_token", request.getAuthToken());
            return ResponseEntity.status(401).body(Map.of("error", "invalid_authorization"));
        }

        // Verificar nuevamente saldo antes de ejecutar
        Optional<BigDecimal> saldoActual = cuentaService.obtenerSaldo(request.getCuentaId());
        
        if (saldoActual.isEmpty() || saldoActual.get().compareTo(request.getMonto()) < 0) {
            return ResponseEntity.status(400).body(Map.of("error", "insufficient_funds"));
        }

        // Calcular nuevo saldo
        BigDecimal nuevoSaldo = saldoActual.get().subtract(request.getMonto());

        // Ejecutar la transacción
        boolean transaccionExitosa = cuentaService.actualizarSaldo(request.getCuentaId(), nuevoSaldo);

        if (!transaccionExitosa) {
            logSecurityEvent(atmId, "transaction_failed", request.getCuentaId().toString());
            return ResponseEntity.status(500).body(Map.of("error", "transaction_failed"));
        }

        // Log de auditoría para retiro exitoso
        String operationId = generarOperationId();
        logATMOperation(atmId, sessionId, "withdrawal_completed", 
            String.format("Account: %d, Amount: %s, OpId: %s", 
                request.getCuentaId(), request.getMonto(), operationId));

        Map<String, Object> response = new HashMap<>();
        response.put("transaction_successful", true);
        response.put("operation_id", operationId);
        response.put("amount_withdrawn", request.getMonto());
        response.put("new_balance", formatearSaldoATM(nuevoSaldo));
        response.put("timestamp", System.currentTimeMillis());
        response.put("receipt_number", generarReceiptNumber());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Últimas transacciones", 
               description = "Consultar últimas transacciones de la cuenta")
    @GetMapping("/transacciones/{cuentaId}")
    @PreAuthorize("hasRole('ATM')")
    public ResponseEntity<Map<String, Object>> obtenerUltimasTransacciones(
            @Parameter(description = "ID de la cuenta") 
            @PathVariable Long cuentaId,
            
            @Parameter(description = "Número de transacciones (máx: 10)") 
            @RequestParam(defaultValue = "5") int limit,
            
            @RequestHeader("ATM-ID") String atmId,
            @RequestHeader("Session-ID") String sessionId) {

        // Limitar consultas para ATM
        int limiteFinal = Math.min(limit, 10);

        // Validar cuenta activa
        if (!cuentaService.validarCuentaActiva(cuentaId)) {
            return ResponseEntity.status(404).body(Map.of("error", "account_not_found"));
        }

        // En un sistema real, obtendríamos las transacciones de esta cuenta específica
        // Por ahora, simulamos con datos básicos
        Map<String, Object> response = new HashMap<>();
        response.put("account_id", cuentaId);
        response.put("transactions_available", true);
        response.put("max_displayed", limiteFinal);
        response.put("note", "Últimas transacciones disponibles en comprobante");

        logATMOperation(atmId, sessionId, "transaction_history", cuentaId.toString());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Estado del ATM", 
               description = "Verificar estado operativo del ATM")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> obtenerEstadoATM(
            @RequestHeader("ATM-ID") String atmId) {

        Map<String, Object> status = new HashMap<>();
        status.put("atm_id", atmId);
        status.put("operational", true);
        status.put("cash_available", true);
        status.put("printer_status", "online");
        status.put("last_maintenance", "2025-09-01");
        status.put("max_withdrawal", 500000);

        return ResponseEntity.ok(status);
    }

    // Métodos auxiliares de validación y seguridad

    private boolean isValidATMOperation(String atmId, String sessionId) {
        return atmId != null && atmId.matches("ATM-\\d{6}") && 
               sessionId != null && sessionId.length() > 10;
    }

    private Map<String, String> validarOperacionRetiro(RetiroValidationRequest request, String atmId, String sessionId) {
        Map<String, String> errors = new HashMap<>();

        if (request.getCuentaId() == null) {
            errors.put("account_id", "Account ID is required");
        }

        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            errors.put("amount", "Invalid amount");
        }

        if (request.getMonto() != null && request.getMonto().compareTo(new BigDecimal("500000")) > 0) {
            errors.put("amount", "Amount exceeds daily limit");
        }

        // Validar múltiplos de billetes disponibles (ej: múltiplos de 1000)
        if (request.getMonto() != null && request.getMonto().remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) != 0) {
            errors.put("amount", "Amount must be in multiples of 1000");
        }

        return errors;
    }

    private boolean isValidAuthToken(String authToken) {
        // En producción, verificar token contra cache temporal
        return authToken != null && authToken.length() > 20;
    }

    private String formatearSaldoATM(BigDecimal saldo) {
        return String.format("$%,.0f", saldo);
    }

    private String generarOperationId() {
        return "OP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generarReceiptNumber() {
        return "REC-" + System.currentTimeMillis();
    }

    // Métodos de auditoría y logging

    private void logATMOperation(String atmId, String sessionId, String operation, String details) {
        System.out.println(String.format("[ATM-AUDIT] ATM: %s | Session: %s | Op: %s | Details: %s | Time: %s",
            atmId, sessionId, operation, details, LocalDateTime.now()));
    }

    private void logSecurityEvent(String atmId, String event, String details) {
        System.out.println(String.format("[ATM-SECURITY] ATM: %s | Event: %s | Details: %s | Time: %s",
            atmId, event, details, LocalDateTime.now()));
    }

    // DTOs para operaciones ATM

    public static class RetiroValidationRequest {
        private Long cuentaId;
        private BigDecimal monto;
        private String tipoOperacion = "withdrawal";

        // Getters y Setters
        public Long getCuentaId() { return cuentaId; }
        public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }
        
        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }
        
        public String getTipoOperacion() { return tipoOperacion; }
        public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }

        @Override
        public String toString() {
            return String.format("Account: %d, Amount: %s", cuentaId, monto);
        }
    }

    public static class RetiroExecutionRequest {
        private Long cuentaId;
        private BigDecimal monto;
        private String authToken;

        // Getters y Setters
        public Long getCuentaId() { return cuentaId; }
        public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }
        
        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }
        
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
    }
}
