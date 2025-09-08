package com.duoc.batch_demo.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utilidad para manejo de tokens JWT.
 * Proporciona funcionalidades comunes para todos los BFFs.
 */
@Component
public class JwtTokenUtil {

    // Clave secreta para firmar tokens (en producción usar configuración externa)
    // Clave de 512 bits (64 bytes) para HS512 - cumple con RFC 7518
    private final String SECRET = "bffBankingSecretKeyForDuocProject2025VeryLongAndSecureKeyThatMeetsThe512BitRequirementForHS512Algorithm";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // Tiempos de expiración por tipo de cliente
    private static final long WEB_EXPIRATION_TIME = 2 * 60 * 60 * 1000; // 2 horas
    private static final long MOBILE_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000; // 7 días
    private static final long ATM_EXPIRATION_TIME = 5 * 60 * 1000; // 5 minutos

    /**
     * Genera token para BFF Web
     */
    public String generateWebToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_type", "WEB");
        claims.put("role", role);
        return createToken(claims, username, WEB_EXPIRATION_TIME);
    }

    /**
     * Genera token para BFF Móvil
     */
    public String generateMobileToken(String username, String deviceId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_type", "MOBILE");
        claims.put("device_id", deviceId);
        claims.put("role", role);
        return createToken(claims, username, MOBILE_EXPIRATION_TIME);
    }

    /**
     * Genera token para BFF ATM (ultra seguro)
     */
    public String generateATMToken(String cardNumber, String atmId, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_type", "ATM");
        claims.put("atm_id", atmId);
        claims.put("session_id", sessionId);
        claims.put("card_last_four", cardNumber.substring(cardNumber.length() - 4));
        return createToken(claims, "ATM_USER", ATM_EXPIRATION_TIME);
    }

    /**
     * Crea el token JWT con los claims especificados
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Valida si el token es válido
     */
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * Valida token específico para ATM
     */
    public Boolean validateATMToken(String token, String atmId, String sessionId) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String tokenAtmId = (String) claims.get("atm_id");
            String tokenSessionId = (String) claims.get("session_id");
            String clientType = (String) claims.get("client_type");
            
            return "ATM".equals(clientType) && 
                   atmId.equals(tokenAtmId) && 
                   sessionId.equals(tokenSessionId) && 
                   !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene el nombre de usuario del token
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Obtiene la fecha de expiración del token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Obtiene el tipo de cliente del token
     */
    public String getClientTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("client_type");
    }

    /**
     * Obtiene un claim específico del token
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Obtiene todos los claims del token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Verifica si el token ha expirado
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Obtiene información del dispositivo móvil del token
     */
    public String getDeviceIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("device_id");
    }

    /**
     * Obtiene el ID de sesión ATM del token
     */
    public String getSessionIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("session_id");
    }

    /**
     * Verifica si el token pertenece a un tipo específico de cliente
     */
    public boolean isTokenForClientType(String token, String expectedClientType) {
        try {
            String actualClientType = getClientTypeFromToken(token);
            return expectedClientType.equals(actualClientType);
        } catch (Exception e) {
            return false;
        }
    }
}
