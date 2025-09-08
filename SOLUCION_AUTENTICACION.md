# Resolución de Problemas de Autenticación - Sistema BFF

## ✅ Problema Resuelto: Error 403 en Endpoints de Autenticación

### 🔍 Diagnóstico del Problema
El sistema presentaba errores 403 (Forbidden) al intentar acceder a los endpoints de autenticación BFF, incluso para rutas que deberían ser públicas como `/api/web/auth/login`.

### 🎯 Causa Raíz Identificada
La configuración de Spring Security en `SecurityConfig.java` tenía reglas de autorización conflictivas:
1. **Orden incorrecto de reglas**: Las reglas más específicas no estaban correctamente organizadas
2. **Manejo de excepciones faltante**: No había configuración de `authenticationEntryPoint` apropiada  
3. **JWT secret key inválido**: Clave de 456 bits insuficiente para algoritmo HS512 (requiere ≥512 bits)

### 🛠️ Solución Implementada

#### 1. **Reorganización de Reglas de Autorización**
```java
.authorizeHttpRequests(authz -> authz
    // Rutas públicas - NO requieren autenticación
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/h2-console/**").permitAll()
    
    // Endpoints de autenticación BFF - NO requieren autenticación previa
    .requestMatchers("/api/web/auth/login").permitAll()
    .requestMatchers("/api/mobile/auth/login").permitAll()
    .requestMatchers("/api/atm/auth/validate-card").permitAll()
    
    // Endpoints BFF protegidos - requieren autenticación JWT
    .requestMatchers("/api/web/**").authenticated()
    .requestMatchers("/api/mobile/**").authenticated()
    .requestMatchers("/api/atm/**").authenticated()
    
    // Otras rutas por defecto son permitidas
    .anyRequest().permitAll()
)
```

#### 2. **Configuración de Manejo de Excepciones**
```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token requerido para acceder a esta ruta BFF\"}");
    })
    .accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setStatus(403);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Acceso denegado para esta ruta BFF\"}");
    })
)
```

#### 3. **Corrección de JWT Secret Key**
```java
// Antes: 456 bits (INVÁLIDO para HS512)
private final String SECRET = "bffBankingSecretKeyForDuocProject2025VeryLongAndSecureKey";

// Después: 512+ bits (VÁLIDO para HS512)  
private final String SECRET = "bffBankingSecretKeyForDuocProject2025VeryLongAndSecureKeyThatMeetsThe512BitRequirementForHS512Algorithm";
```

### 🎯 Cumplimiento del Requisito: "Auth aplicado SOLO a los BFFs"

La configuración actualizada respeta el requisito de que la autenticación se aplique únicamente a los BFFs:

- ✅ **Rutas BFF protegidas**: `/api/web/**`, `/api/mobile/**`, `/api/atm/**` (requieren JWT)
- ✅ **Endpoints de autenticación públicos**: Login endpoints accesibles sin token
- ✅ **Rutas no-BFF públicas**: Actuator, Swagger, H2 Console (sin autenticación)
- ✅ **Otras rutas**: Por defecto públicas (no bloquean funcionalidad de batch)

### ✅ Validación de la Solución

#### Endpoints de Autenticación (Funcionando ✅)
```bash
# Web BFF Login
curl -X POST http://localhost:8080/api/web/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# ✅ Respuesta: JWT token + información de usuario

# Mobile BFF Login  
curl -X POST http://localhost:8080/api/mobile/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123","deviceId":"mobile-device-123"}'
# ✅ Respuesta: JWT token móvil con 7 días de duración

# ATM BFF Card Validation
curl -X POST http://localhost:8080/api/atm/auth/validate-card \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4532015112830366","atmId":"ATM-123456"}'
# ✅ Respuesta: Sesión temporal para entrada de PIN
```

#### Endpoints Protegidos (Funcionando ✅)
```bash
# Sin token: 401 Unauthorized
curl http://localhost:8080/api/web/cuentas
# ✅ Respuesta: {"error":"Unauthorized","message":"Token requerido para acceder a esta ruta BFF"}

# Con token: Acceso permitido
curl -H "Authorization: Bearer <JWT_TOKEN>" http://localhost:8080/api/web/auth/me
# ✅ Respuesta: Información del usuario autenticado
```

#### Endpoints Públicos (Funcionando ✅)
```bash
# Actuator
curl http://localhost:8080/actuator/health
# ✅ Respuesta: Estado de salud del sistema

# Swagger UI
curl http://localhost:8080/swagger-ui.html
# ✅ Respuesta: Redirección a documentación API
```

### 🔄 Para Usar con MySQL en Producción

Para usar con la base de datos MySQL original, ejecutar:
```bash
mvn spring-boot:run
# (sin perfil H2)
```

Para testing rápido con H2:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### 📝 Archivos Modificados

1. **`SecurityConfig.java`** - Reorganización de reglas de autorización
2. **`JwtTokenUtil.java`** - Corrección de secret key para HS512
3. **`application.properties`** - Documentación de configuración BFF
4. **`application-h2.properties`** - Perfil de testing (nuevo)

### 🎯 Resultado Final

- ❌ **Antes**: Error 403 en todos los endpoints de autenticación
- ✅ **Después**: Autenticación funcionando para todos los BFFs (Web, Mobile, ATM)
- ✅ **Cumple requisito**: Auth aplicado SOLO a BFFs, no afecta funcionalidad batch
- ✅ **Sin contraseñas auto-generadas**: Custom UserDetailsService funcionando correctamente