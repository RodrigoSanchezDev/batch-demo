# 🏦 Sistema Bancario BFF (Backend for Frontend)

## 📖 Descripción del Proyecto

Este proyecto implementa una **estrategia BFF (Backend for Frontend)** completa para un sistema bancario, proporcionando APIs especializadas para tres tipos diferentes de clientes: **Web**, **Móvil** y **ATM**. Cada BFF está optimizado para las necesidades específicas de su canal de acceso.

## 🏗️ Arquitectura BFF

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   BFF-WEB       │    │   BFF-MOBILE    │    │   BFF-ATM       │
│   (Puerto 8081) │    │   (Puerto 8082) │    │   (Puerto 8083) │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • Datos completos│    │ • Datos ligeros │    │ • Datos críticos│
│ • Paginación    │    │ • Compresión    │    │ • Encriptación  │
│ • Filtros avanz.│    │ • Cache agresivo│    │ • Validaciones  │
│ • Reportes      │    │ • Resúmenes     │    │ • Transacciones │
└─────────────────┘    └─────────────────┘    └─────────────────┘
           │                      │                      │
           └──────────────────────┼──────────────────────┘
                                  │
                    ┌─────────────────────────┐
                    │    CORE BATCH SERVICE   │
                    │    (Puerto 8080)        │
                    │ • Procesamiento Batch   │
                    │ • Base de Datos         │
                    │ • Lógica de Negocio     │
                    └─────────────────────────┘
```

## 🎯 Justificación de la Estrategia BFF

### ¿Por qué BFF para este Proyecto?

1. **Diversidad de Clientes**: Web, Móvil, ATM tienen necesidades completamente diferentes
2. **Volumen de Datos**: 3000+ registros requieren optimización específica por canal
3. **Seguridad Diferenciada**: ATMs necesitan mayor seguridad que Web/Móvil
4. **Performance**: Cada cliente requiere diferentes niveles de agregación y filtrado

### Beneficios Implementados

- ✅ **Performance Optimizada**: Cada BFF entrega exactamente lo que cada cliente necesita
- ✅ **Escalabilidad Independiente**: Puedes escalar cada BFF según demanda
- ✅ **Mantenimiento Separado**: Cambios en web no afectan móvil o ATM
- ✅ **Seguridad Granular**: Diferentes niveles según criticidad del canal

## 🚀 Características por BFF

### 🌐 BFF Web (Puerto 8081)
**Optimizado para navegadores con datos completos**

- **Datos completos** con historial detallado
- **Paginación avanzada** (50-100 registros por página)
- **Filtros complejos** y búsquedas
- **Reportes customizables** con exportación
- **Gráficos y visualizaciones** de datos

**APIs Principales:**
```
GET /api/web/transacciones?page=0&size=50&filters=...
GET /api/web/cuentas/detalle/{id}?includeHistorial=true
GET /api/web/reportes/anomalias?fechaDesde=...&fechaHasta=...
GET /api/web/dashboard/resumen-ejecutivo
POST /api/web/reportes/custom
```

### 📱 BFF Móvil (Puerto 8082)
**Respuestas ligeras para reducir ancho de banda**

- **Datos esenciales** sin campos innecesarios
- **Respuestas comprimidas** (GZIP)
- **Cache agresivo** (Redis/In-memory)
- **Límites de registros** (máximo 20 por request)
- **Push notifications** para anomalías

**APIs Principales:**
```
GET /api/mobile/cuentas/resumen
GET /api/mobile/transacciones/recientes?limit=10
GET /api/mobile/saldo/{cuentaId}
GET /api/mobile/notificaciones/anomalias
POST /api/mobile/transferencia/quick
```

### 🏧 BFF ATM (Puerto 8083)
**Interfaz segura para operaciones críticas**

- **Seguridad extrema** con tokens específicos ATM
- **Validaciones estrictas** antes de operaciones
- **Logs de auditoría** completos
- **Timeouts cortos** para sesiones
- **Datos mínimos** (solo lo necesario para operación)

**APIs Principales:**
```
POST /api/atm/auth/validate-card
GET /api/atm/saldo/{cuentaId}/secure
POST /api/atm/retiro/validate
POST /api/atm/retiro/execute
GET /api/atm/transacciones/ultimas/{limit}
```

## 🔐 Sistema de Autenticación Diferenciado

### Estrategia de Seguridad por BFF

| BFF | Método | Duración | Características |
|-----|--------|----------|----------------|
| **Web** | JWT + Session | 2 horas | Multi-factor opcional, refresh tokens |
| **Móvil** | OAuth2 + Biometric | 7 días | Tokens largos, integración biométrica |
| **ATM** | Card + PIN + Encryption | 5 minutos | Validación física, encriptación E2E |

## 🛠️ Tecnologías Utilizadas

- **Framework**: Spring Boot 3.5.4
- **Seguridad**: Spring Security + JWT
- **Documentación**: OpenAPI/Swagger
- **Base de Datos**: MySQL/Oracle
- **Cache**: Redis (para móvil)
- **Tokens**: JJWT 0.11.5
- **Java**: 17

## 📊 Datos del Sistema

El sistema procesa datos reales del batch existente:

- **Transacciones**: 3000+ registros distribuidos en semanas
- **Cuentas**: Información completa de clientes
- **Anomalías**: Detección automática de transacciones sospechosas
- **Intereses**: Cálculos automáticos mensuales

## 🚀 Instalación y Ejecución

### Prerrequisitos

```bash
# Java 17
java -version

# MySQL (o usar H2 para desarrollo)
mysql --version

# Redis (opcional para cache móvil)
redis-server --version
```

### Ejecución

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd batch-demo
```

2. **Configurar base de datos**
```bash
# Crear base de datos MySQL
mysql -u root -p
CREATE DATABASE banco_batch;
```

3. **Ejecutar la aplicación**
```bash
./mvnw spring-boot:run
```

4. **Acceder a las APIs**
- **Documentación Swagger**: http://localhost:8080/swagger-ui.html
- **BFF Web**: http://localhost:8080/api/web/*
- **BFF Móvil**: http://localhost:8080/api/mobile/*
- **BFF ATM**: http://localhost:8080/api/atm/*

## 📝 Endpoints de Prueba

### Autenticación Web
```bash
curl -X POST http://localhost:8080/api/web/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### Autenticación Móvil
```bash
curl -X POST http://localhost:8080/api/mobile/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "mobile123", "deviceId": "device123"}'
```

### Autenticación ATM
```bash
# Paso 1: Validar tarjeta
curl -X POST http://localhost:8080/api/atm/auth/validate-card \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "1234567890123456", "atmId": "ATM-123456"}'

# Paso 2: Validar PIN
curl -X POST http://localhost:8080/api/atm/auth/validate-pin \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "1234567890123456", "pin": "1234", "atmId": "ATM-123456"}'
```

## 📈 Monitoreo y Métricas

- **Actuator**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/metrics
- **Info**: http://localhost:8080/actuator/info

## 🔍 Logs de Auditoría

El sistema incluye logs específicos para cada tipo de operación:

- **[ATM-AUDIT]**: Operaciones de cajeros automáticos
- **[ATM-SECURITY]**: Eventos de seguridad ATM
- **[BFF-PERFORMANCE]**: Métricas de performance por BFF

## 🧪 Testing

### Usuarios de Prueba

**BFF Web:**
- admin/admin123 (ADMIN)
- analyst/analyst123 (ANALYST)
- viewer/viewer123 (VIEWER)

**BFF Móvil:**
- user1/mobile123 (CUSTOMER)
- user2/mobile456 (PREMIUM_CUSTOMER)
- demo/demo123 (DEMO_USER)

**BFF ATM:**
- Cualquier tarjeta de 16 dígitos con algoritmo Luhn válido
- PIN de 4 dígitos (excepto 0000)

## 📊 Estructura del Proyecto

```
src/main/java/com/duoc/batch_demo/
├── bff/
│   ├── web/controller/          # Controladores BFF Web
│   ├── mobile/controller/       # Controladores BFF Móvil
│   └── atm/controller/          # Controladores BFF ATM
├── security/                    # Configuración de seguridad
├── service/                     # Servicios comunes
├── dto/                         # DTOs específicos
├── model/                       # Modelos de dominio
└── config/                      # Configuraciones
```

## 🤝 Contribuir

1. Fork del proyecto
2. Crear feature branch (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push al branch (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 👥 Autores

- **Rodrigo Sánchez** - Desarrollo Backend III - DUOC UC

---

## 📞 Soporte

Para preguntas o soporte, contactar a través de:
- GitHub Issues
- Email del proyecto

¡Gracias por usar el Sistema Bancario BFF! 🚀
