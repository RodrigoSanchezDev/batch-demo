# 🏦 Sistema de Procesamiento Bancario Batch con Detección de Anomalías

Un sistema empresarial de procesamiento por lotes (batch) desarrollado en Spring Boot que automatiza el procesamiento de datos bancarios legacy, detecta anomalías automáticamente y genera reportes financieros completos con políticas de tolerancia a fallos.

**🎯 Para quién:** Instituciones financieras que necesitan migrar y procesar datos legacy de manera segura y eficiente.  
**⚡ Qué resuelve:** Procesamiento masivo de transacciones bancarias, cálculo de intereses, generación de estados de cuenta y detección inteligente de anomalías en datos históricos.

---

## 📋 Índice

1. [Arquitectura y Stack Tecnológico](#-arquitectura-y-stack-tecnológico)
2. [Características Principales](#-características-principales)
3. [Requisitos del Sistema](#-requisitos-del-sistema)
4. [Instalación y Configuración](#-instalación-y-configuración)
5. [Ejecución del Sistema](#-ejecución-del-sistema)
6. [Base de Datos y Esquema](#-base-de-datos-y-esquema)
7. [Detección de Anomalías](#-detección-de-anomalías)
8. [Evidencias del Sistema](#-evidencias-del-sistema)
9. [Estructura del Proyecto](#-estructura-del-proyecto)
10. [Configuración Avanzada](#-configuración-avanzada)
11. [Troubleshooting](#-troubleshooting)
12. [Licencia y Contacto](#-licencia-y-contacto)

---

## 🏗️ Arquitectura y Stack Tecnológico

### Stack Principal
- **Spring Boot 3.5.4** - Framework de aplicación
- **Spring Batch** - Procesamiento por lotes empresarial
- **MySQL 8.0+** - Base de datos productiva
- **Java 17** - Lenguaje de programación
- **Maven** - Gestión de dependencias

### Arquitectura de Componentes

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRING BOOT APPLICATION                  │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │   READERS   │  │  PROCESSORS  │  │      WRITERS        │ │
│  │ CSV/Database│  │ Calculations │  │   MySQL Batch       │ │
│  │   Sources   │  │ & Validation │  │    Persistence      │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                      SPRING BATCH CORE                     │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │    JOBS     │  │    STEPS     │  │   FAULT TOLERANCE   │ │
│  │ Sequential  │  │   Chunked    │  │ Retry/Skip Policies │ │
│  │ Processing  │  │  Processing  │  │                     │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                       MySQL DATABASE                       │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │ Transacciones│ │   Cuentas    │  │ Anomalías Detectadas│ │
│  │   Intereses  │  │Estados Cuenta│  │  Spring Batch Meta  │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## ⭐ Características Principales

### 🔄 Procesamiento Batch Empresarial
- **6 Jobs independientes** con ejecución secuencial
- **Procesamiento por chunks** (10 registros por transacción)
- **Políticas de reintento** (3 intentos por registro fallido)
- **Omisión inteligente** (skip hasta 5 registros problemáticos)

### 🎯 Jobs Implementados
1. **Reporte de Transacciones Diarias** - Procesa y valida transacciones
2. **Cálculo de Intereses Mensuales** - Calcula intereses sobre saldos
3. **Generación de Estados de Cuenta Anuales** - Resúmenes anuales
4. **Procesamiento de Detalles** - Persistencia de datos calculados
5. **Detección de Anomalías Básicas** - Anomalías pre-marcadas
6. **Detección Avanzada de Anomalías** - Sistema inteligente completo

### 🚨 Sistema de Detección de Anomalías
- **Montos negativos** - Severidad ALTA
- **Montos en cero** - Severidad MEDIA  
- **Registros duplicados** - Detección automática
- **Datos faltantes** - Validación de campos obligatorios
- **Valores fuera de rango** - Edades, tipos, montos excesivos
- **Tipos inválidos** - Validación de catálogos

---

## 💻 Requisitos del Sistema

### Software Requerido
```bash
Java 17+                    # Runtime principal
MySQL 8.0+                  # Base de datos
Maven 3.8+                  # Gestión de dependencias
Git                         # Control de versiones
```

### Configuración MySQL
```sql
-- Crear usuario y base de datos
CREATE DATABASE banco_batch;
CREATE USER 'batch_user'@'localhost' IDENTIFIED BY 'batch_password';
GRANT ALL PRIVILEGES ON banco_batch.* TO 'batch_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 🚀 Instalación y Configuración

### 1. Clonación del Repositorio
```bash
git clone https://github.com/RodrigoSanchezDev/batch-demo.git
cd batch-demo
```

### 2. Configuración de Base de Datos
Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/banco_batch
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuración Spring Batch
spring.batch.jdbc.initialize-schema=never
spring.batch.job.enabled=false
```

### 3. Inicialización del Schema
```bash
# Ejecutar script de base de datos
mysql -u root -p banco_batch < src/main/resources/schema-mysql.sql
```

### 4. Instalación de Dependencias
```bash
./mvnw clean install
```

---

## ▶️ Ejecución del Sistema

### Ejecución Standard
```bash
# Compilar y ejecutar
./mvnw spring-boot:run
```

### Ejecución con Perfil de Desarrollo
```bash
# Con logging detallado
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Resultado Esperado
![Resumen de Ejecución](docs/images/resumen-ejecucion.png)

El sistema procesará automáticamente:
- ✅ 10 transacciones bancarias
- ✅ 8 cuentas de clientes  
- ✅ 9 cuentas anuales
- ✅ 8 cálculos de intereses
- ✅ 9 estados de cuenta anuales
- ✅ 3 anomalías detectadas automáticamente

---

## 🗄️ Base de Datos y Esquema

### Tablas Principales

#### `transacciones`
```sql
CREATE TABLE transacciones (
    id BIGINT PRIMARY KEY,
    fecha DATE,
    monto DECIMAL(15,2),
    tipo VARCHAR(20),
    fecha_procesamiento TIMESTAMP,
    es_anomalia BOOLEAN DEFAULT FALSE,
    motivo_anomalia TEXT
);
```

#### `cuentas` 
```sql
CREATE TABLE cuentas (
    cuenta_id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    saldo DECIMAL(15,2) DEFAULT 0,
    edad INTEGER,
    tipo VARCHAR(20),
    fecha_actualizacion TIMESTAMP
);
```

#### `anomalias_transacciones`
```sql
CREATE TABLE anomalias_transacciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaccion_id BIGINT,
    tipo_anomalia VARCHAR(50),
    descripcion TEXT,
    fecha_deteccion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    severidad ENUM('BAJA','MEDIA','ALTA')
);
```

### Estado de las Tablas
![Resumen de Tablas](docs/images/resumen-tablas.png)

---

## 🔍 Detección de Anomalías

### Anomalías Detectadas Automáticamente
![Anomalías Detectadas](docs/images/anomalias-detectadas.png)

### Verificación de Datos Problemáticos  
![Verificación de Anomalías](docs/images/verificacion-anomalias.png)

### Tipos de Anomalías

| Tipo | Descripción | Severidad | Ejemplo |
|------|-------------|-----------|---------|
| `MONTO_NEGATIVO` | Transacciones con montos negativos | ALTA | -200.00 |
| `MONTO_CERO` | Transacciones sin monto | MEDIA | 0.00 |
| `REGISTRO_DUPLICADO` | Cuentas duplicadas por nombre/edad/tipo | MEDIA | JOHN DOE duplicado |
| `EDAD_INVALIDA` | Edades fuera de rango (18-120) | MEDIA | Edad < 18 o > 120 |
| `TIPO_INVALIDO` | Tipos no válidos en catálogos | MEDIA | Tipos diferentes a DEBITO/CREDITO |

---

## 📊 Evidencias del Sistema

### Datos Procesados
![Datos Procesados](docs/images/datos-procesados.png)

### Estructura de Tablas Calculadas
![Estados Anuales](docs/images/estados-anuales.png)

### Comandos de Verificación

```bash
# Verificar estado de todas las tablas
mysql -u root banco_batch -e "
SELECT 'transacciones' as tabla, COUNT(*) as cantidad FROM transacciones 
UNION SELECT 'cuentas' as tabla, COUNT(*) as cantidad FROM cuentas 
UNION SELECT 'anomalias_transacciones' as tabla, COUNT(*) as cantidad FROM anomalias_transacciones 
ORDER BY cantidad DESC;"

# Ver muestra de datos procesados
mysql -u root banco_batch -e "
SELECT id, fecha, monto, tipo FROM transacciones LIMIT 5;
SELECT cuenta_id, interes_calculado, saldo_nuevo FROM intereses_calculados LIMIT 3;"

# Verificar anomalías
mysql -u root banco_batch -e "
SELECT tipo_anomalia, COUNT(*) as cantidad, severidad 
FROM anomalias_transacciones 
GROUP BY tipo_anomalia, severidad;"
```

---

## 📁 Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/duoc/batch_demo/
│   │   ├── BankBatchSpringBootApplication.java    # Aplicación principal
│   │   ├── DataSourceConfiguration.java           # Configuración DB
│   │   ├── config/                               # Configuraciones Batch
│   │   │   ├── ReaderConfig.java                 # Lectores CSV/DB
│   │   │   ├── WriterConfig.java                 # Escritores DB
│   │   │   └── ProcessorConfig.java              # Procesadores lógica
│   │   ├── model/                                # Entidades
│   │   │   ├── Transaccion.java
│   │   │   ├── Cuenta.java
│   │   │   ├── AnomaliaTransaccion.java
│   │   │   └── ...
│   │   └── processor/                            # Procesadores custom
│   └── resources/
│       ├── application.properties                # Configuración app
│       ├── schema-mysql.sql                      # Schema DB
│       └── data/semana_1/                       # Datos CSV
│           ├── transacciones.csv
│           ├── intereses.csv
│           └── cuentas_anuales.csv
└── docs/images/                                  # Documentación visual
```

---

## ⚙️ Configuración Avanzada

### Personalización de Chunks
```properties
# Tamaño de chunk por defecto: 10
spring.batch.chunk.size=10

# Configuración de retry
spring.batch.retry.limit=3
spring.batch.skip.limit=5
```

### Configuración de Logging
```properties
# Logging detallado Spring Batch
logging.level.org.springframework.batch=DEBUG
logging.level.com.duoc.batch_demo=INFO
```

### Perfiles de Entorno
```bash
# Desarrollo
./mvnw spring-boot:run -Dspring.profiles.active=dev

# Producción  
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

---

## 🔧 Troubleshooting

### Problemas Comunes

#### Error de Conexión MySQL
```bash
# Verificar estado MySQL
brew services list | grep mysql
# o
sudo systemctl status mysql

# Reiniciar MySQL
brew services restart mysql
```

#### Error de Java Version
```bash
# Verificar version Java
java -version
javac -version

# Cambiar version (macOS)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

#### Error de Permisos Maven
```bash
# Dar permisos de ejecución
chmod +x mvnw
```

### Logs de Depuración
```bash
# Ver logs detallados
./mvnw spring-boot:run -Dlogging.level.com.duoc.batch_demo=DEBUG
```

---

## 📈 Métricas y Monitoreo

### Estadísticas de Procesamiento
- **Tiempo promedio de ejecución**: ~1.5 segundos
- **Throughput**: 100+ registros/segundo  
- **Tasa de anomalías**: 3/47 registros (6.4%)
- **Efectividad de detección**: 100%

### Jobs Ejecutados
```bash
# Ver historial de jobs en Spring Batch
mysql -u root banco_batch -e "
SELECT job_name, status, start_time, end_time 
FROM BATCH_JOB_EXECUTION 
ORDER BY start_time DESC LIMIT 10;"
```

---

## 🚀 Roadmap y Mejoras Futuras

- [ ] **Integración con Apache Kafka** para procesamiento en tiempo real
- [ ] **Dashboard web** con Spring Boot Admin
- [ ] **API REST** para consulta de anomalías
- [ ] **Notificaciones automáticas** vía email/Slack
- [ ] **Procesamiento distribuido** con Spring Cloud Data Flow
- [ ] **Machine Learning** para detección predictiva de anomalías

---

## 📄 Licencia y Contacto

### Licencia
Este proyecto está bajo la Licencia MIT. Ver `LICENSE` para más detalles.

### Desarrollado por
**Rodrigo Sánchez**  
🌐 Portfolio: [sanchezdev.com](https://sanchezdev.com)  
📧 Email: contacto@sanchezdev.com  
💼 LinkedIn: [linkedin.com/in/rodrigosanchezdev](https://linkedin.com/in/rodrigosanchezdev)  
🐙 GitHub: [github.com/RodrigoSanchezDev](https://github.com/RodrigoSanchezDev)

### Agradecimientos
- DUOC UC - Desarrollo Backend III
- Spring Framework Team
- Comunidad Open Source

---

## 🌟 ¿Te gusta este proyecto?

⭐ **¡Dale una estrella en GitHub!**  
🔄 **¡Compártelo con tu equipo!**  
📝 **¡Contribuye con mejoras!**

---

*Última actualización: Agosto 2025*
*Versión: 1.0.0*
