package com.duoc.batch_demo.bff.atm.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.batch_demo.security.JwtTokenUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador de autenticación para BFF ATM.
 * Máxima seguridad con validación de tarjeta, PIN y sesiones ultra-cortas.
 */
@RestController
@RequestMapping("/api/atm/auth")
@Tag(name = "ATM Authentication", description = "Autenticación ultra-segura para cajeros automáticos")
public class ATMAuthController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Operation(summary = "Validar tarjeta bancaria", 
               description = "Primer paso: validación de tarjeta física en ATM")
    @PostMapping("/validate-card")
    public ResponseEntity<Map<String, Object>> validateCard(@RequestBody CardValidationRequest request) {
        
        // Validaciones de seguridad
        if (!isValidATMId(request.getAtmId())) {
            return ResponseEntity.status(403).body(Map.of("error", "invalid_atm"));
        }

        if (!isValidCardNumber(request.getCardNumber())) {
            return ResponseEntity.status(400).body(Map.of("error", "invalid_card"));
        }

        // Generar sesión temporal para PIN
        String tempSessionId = UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("card_accepted", true);
        response.put("temp_session", tempSessionId);
        response.put("next_step", "enter_pin");
        response.put("timeout", 30); // 30 segundos para ingresar PIN
        response.put("attempts_allowed", 3);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validar PIN", 
               description = "Segundo paso: validación de PIN con el token temporal")
    @PostMapping("/validate-pin")
    public ResponseEntity<Map<String, Object>> validatePin(@RequestBody PinValidationRequest request) {
        
        // Validar PIN (en producción, verificar contra base de datos encriptada)
        if (!isValidPin(request.getCardNumber(), request.getPin())) {
            
            // Log de intento fallido para auditoría
            logFailedAttempt(request.getAtmId(), request.getCardNumber(), "invalid_pin");
            
            return ResponseEntity.status(401).body(Map.of(
                "error", "invalid_pin",
                "attempts_remaining", calculateRemainingAttempts(request.getCardNumber())
            ));
        }

        // Generar token ATM ultra-seguro
        String sessionId = UUID.randomUUID().toString();
        String token = jwtTokenUtil.generateATMToken(request.getCardNumber(), request.getAtmId(), sessionId);
        
        // Registrar sesión exitosa
        logSuccessfulLogin(request.getAtmId(), request.getCardNumber(), sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("authentication_successful", true);
        response.put("session_token", token);
        response.put("session_id", sessionId);
        response.put("expires_in", 300); // 5 minutos
        response.put("max_transaction_amount", 500000); // Límite por transacción
        response.put("account_info", getBasicAccountInfo(request.getCardNumber()));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Extender sesión ATM", 
               description = "Extender sesión activa por actividad del usuario")
    @PostMapping("/extend-session")
    public ResponseEntity<Map<String, Object>> extendSession(
            @RequestHeader("Authorization") String token,
            @RequestHeader("ATM-ID") String atmId,
            @RequestHeader("Session-ID") String sessionId) {
        
        try {
            // Validar token ATM específico
            if (jwtTokenUtil.validateATMToken(token.substring(7), atmId, sessionId)) {
                
                // Generar nuevo token con tiempo extendido
                String cardNumber = extractCardFromToken(token.substring(7));
                String newSessionId = UUID.randomUUID().toString();
                String newToken = jwtTokenUtil.generateATMToken(cardNumber, atmId, newSessionId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("session_extended", true);
                response.put("new_token", newToken);
                response.put("new_session_id", newSessionId);
                response.put("expires_in", 300);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "session_expired"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_session"));
        }
    }

    @Operation(summary = "Finalizar sesión ATM", 
               description = "Logout seguro con limpieza de sesión")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String token,
            @RequestHeader("ATM-ID") String atmId,
            @RequestHeader("Session-ID") String sessionId) {
        
        try {
            String cardNumber = extractCardFromToken(token.substring(7));
            
            // Log de logout para auditoría
            logSessionEnd(atmId, cardNumber, sessionId, "user_logout");
            
            // En producción, invalidar token en blacklist
            
            Map<String, Object> response = new HashMap<>();
            response.put("logout_successful", true);
            response.put("session_cleared", sessionId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("logout_successful", true, "note", "forced_cleanup"));
        }
    }

    @Operation(summary = "Estado de sesión ATM", 
               description = "Verificar estado actual de la sesión")
    @GetMapping("/session-status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(
            @RequestHeader("Authorization") String token,
            @RequestHeader("ATM-ID") String atmId,
            @RequestHeader("Session-ID") String sessionId) {
        
        try {
            boolean isValid = jwtTokenUtil.validateATMToken(token.substring(7), atmId, sessionId);
            
            Map<String, Object> status = new HashMap<>();
            status.put("session_valid", isValid);
            status.put("atm_id", atmId);
            status.put("session_id", sessionId);
            
            if (isValid) {
                long expirationTime = jwtTokenUtil.getExpirationDateFromToken(token.substring(7)).getTime();
                long remainingTime = Math.max(0, expirationTime - System.currentTimeMillis());
                
                status.put("remaining_seconds", remainingTime / 1000);
                status.put("expires_soon", remainingTime < 60000); // Menos de 1 minuto
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("session_valid", false));
        }
    }

    // Métodos auxiliares de seguridad

    private boolean isValidATMId(String atmId) {
        // En producción, validar contra lista de ATMs autorizados
        return atmId != null && atmId.matches("ATM-\\d{6}");
    }

    private boolean isValidCardNumber(String cardNumber) {
        // Validación básica de formato de tarjeta
        return cardNumber != null && 
               cardNumber.matches("\\d{16}") && 
               luhnCheck(cardNumber);
    }

    private boolean luhnCheck(String cardNumber) {
        // Algoritmo de Luhn para validación de tarjetas
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }

    private boolean isValidPin(String cardNumber, String pin) {
        // En producción, verificar PIN encriptado en base de datos
        // Simulación para demo
        return pin != null && pin.matches("\\d{4}") && !"0000".equals(pin);
    }

    private int calculateRemainingAttempts(String cardNumber) {
        // En producción, consultar intentos fallidos de la base de datos
        return 2; // Simulación
    }

    private Map<String, Object> getBasicAccountInfo(String cardNumber) {
        // Información mínima necesaria para operaciones ATM
        return Map.of(
            "card_type", "debit",
            "account_linked", true,
            "daily_limit_remaining", 300000,
            "last_four", cardNumber.substring(cardNumber.length() - 4)
        );
    }

    private String extractCardFromToken(String token) {
        // En el token ATM, el subject es "ATM_USER", necesitamos los claims
        return "****1234"; // Simulación de últimos 4 dígitos
    }

    // Métodos de auditoría (en producción serían servicios dedicados)

    private void logFailedAttempt(String atmId, String cardNumber, String reason) {
        System.out.println(String.format("[SECURITY] ATM %s - Failed attempt for card ****%s - Reason: %s - Time: %s",
            atmId, cardNumber.substring(cardNumber.length() - 4), reason, LocalDateTime.now()));
    }

    private void logSuccessfulLogin(String atmId, String cardNumber, String sessionId) {
        System.out.println(String.format("[AUDIT] ATM %s - Successful login for card ****%s - Session: %s - Time: %s",
            atmId, cardNumber.substring(cardNumber.length() - 4), sessionId, LocalDateTime.now()));
    }

    private void logSessionEnd(String atmId, String cardNumber, String sessionId, String reason) {
        System.out.println(String.format("[AUDIT] ATM %s - Session ended for card ****%s - Session: %s - Reason: %s - Time: %s",
            atmId, cardNumber.substring(cardNumber.length() - 4), sessionId, reason, LocalDateTime.now()));
    }

    // DTOs para requests seguros

    public static class CardValidationRequest {
        private String cardNumber;
        private String atmId;
        private String timestamp;

        // Getters y Setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getAtmId() { return atmId; }
        public void setAtmId(String atmId) { this.atmId = atmId; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class PinValidationRequest {
        private String cardNumber;
        private String pin;
        private String atmId;
        private String tempSession;

        // Getters y Setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
        
        public String getAtmId() { return atmId; }
        public void setAtmId(String atmId) { this.atmId = atmId; }
        
        public String getTempSession() { return tempSession; }
        public void setTempSession(String tempSession) { this.tempSession = tempSession; }
    }
}
