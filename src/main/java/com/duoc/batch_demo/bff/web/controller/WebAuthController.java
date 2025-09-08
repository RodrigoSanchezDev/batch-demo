package com.duoc.batch_demo.bff.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
 * Controlador de autenticación para BFF Web.
 * Maneja login y gestión de tokens específicos para clientes web.
 */
@RestController
@RequestMapping("/api/web/auth")
@Tag(name = "Web Authentication", description = "Autenticación para clientes web")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"}) // React/Angular
public class WebAuthController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Operation(summary = "Login para cliente web", 
               description = "Autenticación con usuario y contraseña para aplicaciones web")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        // En un sistema real, aquí validarías las credenciales contra la base de datos
        // Por ahora, simulamos una validación simple
        
        if (isValidWebCredentials(loginRequest.getUsername(), loginRequest.getPassword())) {
            String role = determineUserRole(loginRequest.getUsername());
            String token = jwtTokenUtil.generateWebToken(loginRequest.getUsername(), role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            response.put("token_type", "Bearer");
            response.put("expires_in", 7200); // 2 horas en segundos
            response.put("client_type", "WEB");
            response.put("user", Map.of(
                "username", loginRequest.getUsername(),
                "role", role,
                "permissions", getWebPermissions(role)
            ));
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_credentials");
            error.put("error_description", "Usuario o contraseña inválidos");
            return ResponseEntity.status(401).body(error);
        }
    }

    @Operation(summary = "Logout para cliente web")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String token) {
        // En un sistema real, aquí invalidarías el token (blacklist)
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout exitoso");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh token para cliente web")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7); // Remover "Bearer "
            String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            String clientType = jwtTokenUtil.getClientTypeFromToken(jwtToken);
            
            if ("WEB".equals(clientType) && !jwtTokenUtil.isTokenExpired(jwtToken)) {
                String role = determineUserRole(username);
                String newToken = jwtTokenUtil.generateWebToken(username, role);
                
                Map<String, Object> response = new HashMap<>();
                response.put("access_token", newToken);
                response.put("token_type", "Bearer");
                response.put("expires_in", 7200);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "token_refresh_failed"));
        }
    }

    @Operation(summary = "Verificar token y obtener información del usuario")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            String role = determineUserRole(username);
            
            Map<String, Object> user = new HashMap<>();
            user.put("username", username);
            user.put("role", role);
            user.put("client_type", "WEB");
            user.put("permissions", getWebPermissions(role));
            user.put("session_valid", true);
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_session"));
        }
    }

    // Métodos auxiliares para simulación (en producción estos serían servicios reales)
    
    private boolean isValidWebCredentials(String username, String password) {
        // Simulación de validación - en producción usar BCrypt y base de datos
        return ("admin".equals(username) && "admin123".equals(password)) ||
               ("analyst".equals(username) && "analyst123".equals(password)) ||
               ("viewer".equals(username) && "viewer123".equals(password));
    }
    
    private String determineUserRole(String username) {
        switch (username) {
            case "admin": return "ADMIN";
            case "analyst": return "ANALYST";
            case "viewer": return "VIEWER";
            default: return "USER";
        }
    }
    
    private String[] getWebPermissions(String role) {
        switch (role) {
            case "ADMIN":
                return new String[]{"READ_ALL", "WRITE_ALL", "DELETE_ALL", "EXPORT_REPORTS", "MANAGE_USERS"};
            case "ANALYST":
                return new String[]{"READ_ALL", "EXPORT_REPORTS", "CREATE_REPORTS"};
            case "VIEWER":
                return new String[]{"READ_BASIC", "VIEW_DASHBOARD"};
            default:
                return new String[]{"READ_BASIC"};
        }
    }

    // DTOs
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
