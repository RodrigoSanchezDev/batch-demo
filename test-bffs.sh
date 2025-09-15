#!/bin/bash

# ==================================================
# SCRIPT DE PRUEBAS PARA BFFs - SISTEMA BANCARIO
# ==================================================

echo "🔥 INICIANDO PRUEBAS DE BFFs..."
echo "=================================================="
echo ""

# Verificar que el servidor esté ejecutándose
echo "📡 Verificando conexión al servidor HTTPS..."
if ! curl -s -k https://localhost:8443/actuator/health > /dev/null; then
    echo "❌ ERROR: El servidor no está ejecutándose en https://localhost:8443"
    echo "💡 Primero ejecuta: ./mvnw spring-boot:run"
    exit 1
fi

echo "✅ Servidor detectado!"
echo ""

# ==================================================
# PRUEBA 1: BFF WEB - AUTENTICACIÓN
# ==================================================
echo "🌐 PRUEBA 1: BFF Web - Autenticación"
echo "--------------------------------------------------"

WEB_TOKEN=$(curl -s -k -X POST "https://localhost:8443/api/web/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username": "admin", "password": "admin123"}' | \
    grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -n "$WEB_TOKEN" ]; then
    echo "✅ Autenticación Web exitosa!"
    echo "🔑 Token Web obtenido: ${WEB_TOKEN:0:20}..."
else
    echo "❌ Error en autenticación Web"
    WEB_TOKEN="token_not_available"
fi
echo ""

# ==================================================
# PRUEBA 2: BFF WEB - LISTAR TRANSACCIONES
# ==================================================
echo "🌐 PRUEBA 2: BFF Web - Listar Transacciones"
echo "--------------------------------------------------"

curl -s -k -X GET "https://localhost:8443/api/web/transacciones?page=0&size=5" \
    -H "Authorization: Bearer $WEB_TOKEN" \
    -H "Content-Type: application/json"

echo ""
echo "✅ Consulta de transacciones Web completada"
echo ""

# ==================================================
# PRUEBA 3: BFF MOBILE - AUTENTICACIÓN
# ==================================================
echo "📱 PRUEBA 3: BFF Mobile - Autenticación"
echo "--------------------------------------------------"

MOBILE_TOKEN=$(curl -s -k -X POST "https://localhost:8443/api/mobile/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username": "demo", "password": "demo123", "deviceId": "test-device-123"}' | \
    grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -n "$MOBILE_TOKEN" ]; then
    echo "✅ Autenticación Mobile exitosa!"
    echo "🔑 Token Mobile obtenido: ${MOBILE_TOKEN:0:20}..."
else
    echo "❌ Error en autenticación Mobile"
    MOBILE_TOKEN="token_not_available"
fi
echo ""

# ==================================================
# PRUEBA 4: BFF MOBILE - TRANSACCIONES OPTIMIZADAS
# ==================================================
echo "📱 PRUEBA 4: BFF Mobile - Transacciones Optimizadas"
echo "--------------------------------------------------"

curl -s -k -X GET "https://localhost:8443/api/mobile/transacciones/recientes?limite=3" \
    -H "Authorization: Bearer $MOBILE_TOKEN" \
    -H "Device-ID: test-device-123" \
    -H "Content-Type: application/json"

echo ""
echo "✅ Consulta de transacciones Mobile completada"
echo ""

# ==================================================
# PRUEBA 5: BFF ATM - VALIDACIÓN DE TARJETA (Paso 1)
# ==================================================
echo "🏧 PRUEBA 5: BFF ATM - Validación de Tarjeta (Paso 1)"
echo "--------------------------------------------------"

ATM_TEMP_SESSION=$(curl -s -k -X POST "https://localhost:8443/api/atm/auth/validate-card" \
    -H "Content-Type: application/json" \
    -d '{"cardNumber": "4111111111111111", "atmId": "ATM-000001", "location": "Sucursal Centro"}' | \
    grep -o '"temp_session":"[^"]*' | cut -d'"' -f4)

if [ -n "$ATM_TEMP_SESSION" ]; then
    echo "✅ Validación de tarjeta exitosa!"
    echo "🔑 Sesión temporal: ${ATM_TEMP_SESSION:0:20}..."
    
    # Paso 2: Validar PIN
    echo ""
    echo "🏧 PRUEBA 5b: BFF ATM - Validación de PIN (Paso 2)"
    echo "--------------------------------------------------"
    
    ATM_TOKEN=$(curl -s -k -X POST "https://localhost:8443/api/atm/auth/validate-pin" \
        -H "Content-Type: application/json" \
        -d '{"cardNumber": "4111111111111111", "pin": "1234", "atmId": "ATM-000001", "tempSession": "'$ATM_TEMP_SESSION'"}' | \
        grep -o '"session_token":"[^"]*' | cut -d'"' -f4)
    
    if [ -n "$ATM_TOKEN" ]; then
        echo "✅ Validación de PIN exitosa!"
        echo "🔑 Token ATM obtenido: ${ATM_TOKEN:0:20}..."
    else
        echo "❌ Error en validación de PIN"
        ATM_TOKEN="token_not_available"
    fi
else
    echo "❌ Error en validación de tarjeta"
    ATM_TOKEN="token_not_available"
fi
echo ""

# ==================================================
# PRUEBA 6: BFF ATM - CONSULTA DE SALDO
# ==================================================
echo "🏧 PRUEBA 6: BFF ATM - Consulta de Saldo"
echo "--------------------------------------------------"

curl -s -k -X GET "https://localhost:8443/api/atm/operaciones/saldo/1234" \
    -H "Authorization: Bearer $ATM_TOKEN" \
    -H "ATM-ID: ATM-000001" \
    -H "Session-ID: cc35cb3e-2445-4011-86f4-9312827820c8" \
    -H "Content-Type: application/json"

echo ""
echo "✅ Consulta de saldo ATM completada"
echo ""

# ==================================================
# RESUMEN
# ==================================================
echo "🎉 PRUEBAS DE BFFs COMPLETADAS"
echo "=================================================="
echo "🌐 BFF Web: Optimizado para navegadores"
echo "📱 BFF Mobile: Optimizado para apps móviles"
echo "🏧 BFF ATM: Optimizado para cajeros automáticos"
echo ""
echo "📖 Documentación completa: https://localhost:8443/swagger-ui.html"
echo "=================================================="
