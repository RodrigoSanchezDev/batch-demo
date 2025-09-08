package com.duoc.batch_demo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuración de OpenAPI/Swagger para documentar los BFFs.
 * Proporciona documentación interactiva para cada tipo de cliente.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Sistema Bancario BFF - APIs Multi-Cliente")
                .description("""
                    ## Sistema de Backend for Frontend (BFF) Bancario
                    
                    Este sistema proporciona APIs especializadas para diferentes tipos de clientes:
                    
                    ### 🌐 BFF Web (Puerto 8081)
                    - **Audiencia**: Aplicaciones web (React, Angular, etc.)
                    - **Características**: Datos completos, paginación avanzada, reportes
                    - **Autenticación**: JWT con sesiones de 2 horas
                    - **Endpoints**: `/api/web/*`
                    
                    ### 📱 BFF Móvil (Puerto 8082)
                    - **Audiencia**: Aplicaciones móviles nativas
                    - **Características**: Respuestas ligeras, cache agresivo, datos esenciales
                    - **Autenticación**: JWT con sesiones de 7 días + biometría
                    - **Endpoints**: `/api/mobile/*`
                    
                    ### 🏧 BFF ATM (Puerto 8083)
                    - **Audiencia**: Cajeros automáticos
                    - **Características**: Máxima seguridad, sesiones ultra-cortas, operaciones críticas
                    - **Autenticación**: Validación de tarjeta + PIN + tokens de 5 minutos
                    - **Endpoints**: `/api/atm/*`
                    
                    ### 🔐 Seguridad
                    - Cada BFF tiene su propio sistema de autenticación
                    - Tokens JWT específicos por tipo de cliente
                    - Validaciones diferenciadas según criticidad
                    - Logs de auditoría completos
                    
                    ### 📊 Datos
                    Basado en sistema de procesamiento batch con:
                    - 3000+ transacciones procesadas
                    - Detección automática de anomalías
                    - Cálculo de intereses y estados de cuenta
                    - Arquitectura híbrida: Multi-Threading + Partitioning
                    """)
                .version("1.0.0")
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Servidor de desarrollo - Todos los BFFs"),
                new Server()
                    .url("http://localhost:8081")
                    .description("BFF Web - Aplicaciones web"),
                new Server()
                    .url("http://localhost:8082")
                    .description("BFF Móvil - Aplicaciones móviles"),
                new Server()
                    .url("http://localhost:8083")
                    .description("BFF ATM - Cajeros automáticos")
            ))
            
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT específico por tipo de cliente"))
                
                .addSecuritySchemes("ATMAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")
                    .description("Autenticación específica para ATM con headers adicionales"))
            )
            
            .addSecurityItem(new SecurityRequirement()
                .addList("BearerAuth")
                .addList("ATMAuth"));
    }
}
