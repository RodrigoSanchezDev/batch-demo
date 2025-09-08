package com.duoc.batch_demo.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro JWT personalizado para manejar autenticación de los diferentes BFFs.
 * Valida tokens y establece el contexto de seguridad.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();

        String username = null;
        String jwtToken = null;
        String clientType = null;

        // Obtener token del header Authorization
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                clientType = jwtTokenUtil.getClientTypeFromToken(jwtToken);
            } catch (Exception e) {
                logger.warn("No se puede obtener el token JWT: " + e.getMessage());
            }
        }

        // Validar token según el tipo de BFF
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            boolean isValidToken = false;
            
            // Validación específica por tipo de cliente
            if ("WEB".equals(clientType) && requestURI.startsWith("/api/web/")) {
                isValidToken = jwtTokenUtil.validateToken(jwtToken, username);
            } 
            else if ("MOBILE".equals(clientType) && requestURI.startsWith("/api/mobile/")) {
                isValidToken = jwtTokenUtil.validateToken(jwtToken, username);
            } 
            else if ("ATM".equals(clientType) && requestURI.startsWith("/api/atm/")) {
                // Para ATM, necesitamos validaciones adicionales
                String atmId = request.getHeader("ATM-ID");
                String sessionId = request.getHeader("Session-ID");
                
                if (atmId != null && sessionId != null) {
                    isValidToken = jwtTokenUtil.validateATMToken(jwtToken, atmId, sessionId);
                }
            }

            // Si el token es válido, establecer autenticación
            if (isValidToken) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        username, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + clientType))
                    );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Excluir rutas públicas de la autenticación
        return path.startsWith("/api/public/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/actuator/") ||
               path.equals("/api/web/auth/login") ||
               path.equals("/api/mobile/auth/login") ||
               path.equals("/api/atm/auth/validate-card");
    }
}
