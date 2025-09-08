package com.duoc.batch_demo.bff.mobile.controller;

import java.util.HashMap;
import java.util.Map;

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
 * Controlador de autenticación para BFF Móvil.
 * Optimizado para dispositivos móviles con tokens de larga duración.
 */
@RestController
@RequestMapping("/api/mobile/auth")
@Tag(name = "Mobile Authentication", description = "Autenticación para clientes móviles")
public class MobileAuthController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Operation(summary = "Login móvil", 
               description = "Autenticación optimizada para dispositivos móviles con tokens de larga duración")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody MobileLoginRequest loginRequest) {
        
        if (isValidMobileCredentials(loginRequest.getUsername(), loginRequest.getPassword())) {
            String role = determineUserRole(loginRequest.getUsername());
            String token = jwtTokenUtil.generateMobileToken(
                loginRequest.getUsername(), 
                loginRequest.getDeviceId(), 
                role
            );
            
            // Respuesta optimizada para móvil (datos mínimos)
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("expires", 7 * 24 * 60 * 60); // 7 días en segundos
            response.put("user", Map.of(
                "id", loginRequest.getUsername(),
                "role", role
            ));
            response.put("device_registered", true);
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_login");
            error.put("message", "Credenciales inválidas");
            return ResponseEntity.status(401).body(error);
        }
    }

    @Operation(summary = "Login con biometría", 
               description = "Autenticación usando datos biométricos del dispositivo")
    @PostMapping("/biometric-login")
    public ResponseEntity<Map<String, Object>> biometricLogin(@RequestBody BiometricLoginRequest request) {
        
        // En un sistema real, validarías el hash biométrico contra la base de datos
        if (isValidBiometricData(request.getUserId(), request.getBiometricHash(), request.getDeviceId())) {
            String role = determineUserRole(request.getUserId());
            String token = jwtTokenUtil.generateMobileToken(request.getUserId(), request.getDeviceId(), role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("auth_method", "biometric");
            response.put("user", Map.of("id", request.getUserId(), "role", role));
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "biometric_failed"));
        }
    }

    @Operation(summary = "Refresh token móvil")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestHeader("Authorization") String token,
            @RequestHeader("Device-ID") String deviceId) {
        
        try {
            String jwtToken = token.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            String tokenDeviceId = jwtTokenUtil.getDeviceIdFromToken(jwtToken);
            
            // Verificar que el device ID coincida
            if (deviceId.equals(tokenDeviceId) && !jwtTokenUtil.isTokenExpired(jwtToken)) {
                String role = determineUserRole(username);
                String newToken = jwtTokenUtil.generateMobileToken(username, deviceId, role);
                
                return ResponseEntity.ok(Map.of(
                    "token", newToken,
                    "type", "Bearer",
                    "refreshed", true
                ));
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "device_mismatch"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "refresh_failed"));
        }
    }

    @Operation(summary = "Logout móvil")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String token,
            @RequestHeader("Device-ID") String deviceId) {
        
        // En un sistema real, invalidarías el token y limpiarías datos de sesión
        return ResponseEntity.ok(Map.of(
            "message", "Logout exitoso",
            "device_cleared", deviceId
        ));
    }

    @Operation(summary = "Verificar estado de sesión")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            String deviceId = jwtTokenUtil.getDeviceIdFromToken(jwtToken);
            boolean isExpired = jwtTokenUtil.isTokenExpired(jwtToken);
            
            Map<String, Object> status = new HashMap<>();
            status.put("valid", !isExpired);
            status.put("user", username);
            status.put("device", deviceId);
            status.put("expires_soon", willExpireSoon(jwtToken));
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_session"));
        }
    }

    // Métodos auxiliares de validación (simulación)
    
    private boolean isValidMobileCredentials(String username, String password) {
        return ("user1".equals(username) && "mobile123".equals(password)) ||
               ("user2".equals(username) && "mobile456".equals(password)) ||
               ("demo".equals(username) && "demo123".equals(password));
    }
    
    private boolean isValidBiometricData(String userId, String biometricHash, String deviceId) {
        // En producción, verificarías el hash biométrico almacenado para este usuario y dispositivo
        return biometricHash != null && biometricHash.length() > 10 && deviceId != null;
    }
    
    private String determineUserRole(String username) {
        return switch (username) {
            case "user1" -> "CUSTOMER";
            case "user2" -> "PREMIUM_CUSTOMER";
            case "demo" -> "DEMO_USER";
            default -> "BASIC_USER";
        };
    }
    
    private boolean willExpireSoon(String token) {
        try {
            long expirationTime = jwtTokenUtil.getExpirationDateFromToken(token).getTime();
            long currentTime = System.currentTimeMillis();
            long timeUntilExpiration = expirationTime - currentTime;
            
            // Considerar "pronto a expirar" si queda menos de 1 día
            return timeUntilExpiration < (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return true;
        }
    }

    // DTOs optimizados para móvil
    
    public static class MobileLoginRequest {
        private String username;
        private String password;
        private String deviceId;
        private String deviceModel;
        private String appVersion;

        // Getters y Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getDeviceModel() { return deviceModel; }
        public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
        
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    }

    public static class BiometricLoginRequest {
        private String userId;
        private String biometricHash;
        private String deviceId;
        private String biometricType; // fingerprint, face, etc.

        // Getters y Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getBiometricHash() { return biometricHash; }
        public void setBiometricHash(String biometricHash) { this.biometricHash = biometricHash; }
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getBiometricType() { return biometricType; }
        public void setBiometricType(String biometricType) { this.biometricType = biometricType; }
    }
}
