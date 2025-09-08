# ==============================================
# GUÍA DE EJECUCIÓN DEL PROYECTO BFF
# ==============================================

Este proyecto implementa una estrategia de Backend for Frontend (BFF) para un sistema bancario
con tres clientes diferenciados: Web, Mobile y ATM.

## 🚀 EJECUCIÓN RÁPIDA

### Opción 1: Scripts Automatizados (RECOMENDADO)

1. **Iniciar el servidor** (Terminal 1):
```bash
./start-server.sh
```

2. **Probar los BFFs** (Terminal 2):
```bash
./test-bffs.sh
```

### Opción 2: Ejecución Manual

1. **Compilar el proyecto**:
```bash
./mvnw clean compile
```

2. **Iniciar servidor** (dejar corriendo):
```bash
./mvnw spring-boot:run
```

3. **En otra terminal, probar endpoints**:

#### BFF Web (navegadores)
```bash
# Autenticación
curl -X POST "http://localhost:8080/api/web/auth/login" \
-H "Content-Type: application/json" \
-d '{"usuario": "admin", "password": "admin123"}'

# Listar transacciones (reemplaza TOKEN)
curl -X GET "http://localhost:8080/api/web/transacciones?page=0&size=5" \
-H "Authorization: Bearer TOKEN"
```

#### BFF Mobile (apps móviles)
```bash
# Autenticación móvil
curl -X POST "http://localhost:8080/api/mobile/auth/login" \
-H "Content-Type: application/json" \
-d '{"usuario": "mobile_user", "password": "mobile123", "dispositivo": "iPhone13", "version": "1.2.3"}'

# Transacciones recientes optimizadas
curl -X GET "http://localhost:8080/api/mobile/transacciones/recientes?limite=3" \
-H "Authorization: Bearer TOKEN"
```

#### BFF ATM (cajeros automáticos)
```bash
# Autenticación con tarjeta
curl -X POST "http://localhost:8080/api/atm/auth/card" \
-H "Content-Type: application/json" \
-d '{"numeroTarjeta": "4532123456789012", "pin": "1234", "atmId": "ATM001"}'

# Consulta de saldo
curl -X GET "http://localhost:8080/api/atm/saldo" \
-H "Authorization: Bearer TOKEN"
```

### Opción 3: Usando tmux/screen (Avanzado)

Si tienes `tmux` instalado:

```bash
# Crear sesión tmux
tmux new-session -d -s banco-bff

# Dividir ventana
tmux split-window -h

# Ejecutar servidor en panel izquierdo
tmux send-keys -t banco-bff:0.0 './start-server.sh' Enter

# Ejecutar pruebas en panel derecho (esperar a que el servidor inicie)
tmux send-keys -t banco-bff:0.1 'sleep 5 && ./test-bffs.sh' Enter

# Conectar a la sesión
tmux attach-session -t banco-bff
```

### Opción 4: VS Code con múltiples terminales

1. Abre VS Code
2. Terminal → New Terminal (Ctrl + Shift + `)
3. Ejecuta: `./start-server.sh`
4. Terminal → Split Terminal (Ctrl + Shift + 5)
5. En la nueva terminal: `./test-bffs.sh`

## 📖 DOCUMENTACIÓN INTERACTIVA

Una vez que el servidor esté ejecutándose:
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator Health: http://localhost:8080/actuator/health

## 🔧 DIFERENCIAS ENTRE BFFs

| Característica | Web BFF | Mobile BFF | ATM BFF |
|----------------|---------|------------|---------|
| **Sesión JWT** | 2 horas | 7 días | 5 minutos |
| **Datos por página** | 50-100 | 10-20 | 1-5 |
| **Autenticación** | Usuario/Password | Biométrica + Device | Tarjeta + PIN |
| **Cache** | Navegador | Redis | Sin cache |
| **Formato respuesta** | Completo | Comprimido | Mínimo |

## 🛡️ SEGURIDAD

- **JWT Tokens**: Diferentes duraciones por cliente
- **CORS**: Configurado para cada BFF
- **Rate Limiting**: Implementado por tipo de cliente
- **Validaciones**: Luhn algorithm para tarjetas ATM

## 🏗️ ARQUITECTURA

```
Cliente Web ──────► BFF Web ──────┐
                                   │
Cliente Móvil ────► BFF Mobile ───┼──► Servicios Core
                                   │    (TransaccionService,
Cliente ATM ──────► BFF ATM ──────┘     CuentaService)
```

## ❗ TROUBLESHOOTING

**Puerto 8080 ocupado:**
```bash
kill $(lsof -t -i:8080)
```

**Error de compilación:**
```bash
./mvnw clean install
```

**Base de datos no disponible:**
Verificar que MySQL esté ejecutándose en puerto 3306.

## 📋 CHECKLIST DE PRUEBAS

- [ ] Servidor inicia correctamente
- [ ] BFF Web: Autenticación exitosa
- [ ] BFF Web: Consulta de transacciones
- [ ] BFF Mobile: Autenticación con dispositivo
- [ ] BFF Mobile: Transacciones optimizadas
- [ ] BFF ATM: Autenticación con tarjeta
- [ ] BFF ATM: Consulta de saldo
- [ ] Documentación Swagger accesible
- [ ] Diferentes tiempos de expiración JWT
