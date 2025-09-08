#!/bin/bash

# ==================================================
# SCRIPT PARA EJECUTAR EL SERVIDOR BFF
# ==================================================

echo "🏦 INICIANDO SERVIDOR BANCARIO BFF..."
echo "=================================================="
echo ""

# Verificar si el puerto 8080 está ocupado
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo "⚠️  Puerto 8080 ya está en uso"
    echo "🔍 Procesos usando el puerto 8080:"
    lsof -Pi :8080 -sTCP:LISTEN
    echo ""
    echo "💡 Para detener el proceso existente:"
    echo "   kill \$(lsof -t -i:8080)"
    echo ""
    read -p "¿Deseas continuar de todas formas? (y/N): " respuesta
    if [[ ! $respuesta =~ ^[Yy]$ ]]; then
        echo "❌ Operación cancelada"
        exit 1
    fi
fi

echo "🚀 Compilando proyecto..."
./mvnw clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
else
    echo "❌ Error en compilación"
    exit 1
fi

echo ""
echo "🌟 Iniciando servidor en modo BFF..."
echo "=================================================="
echo "🌐 API Web BFF: http://localhost:8080/api/web/"
echo "📱 API Mobile BFF: http://localhost:8080/api/mobile/"
echo "🏧 API ATM BFF: http://localhost:8080/api/atm/"
echo "📖 Documentación: http://localhost:8080/swagger-ui.html"
echo "=================================================="
echo ""
echo "💡 Para probar los endpoints, ejecuta en otra terminal:"
echo "   ./test-bffs.sh"
echo ""
echo "💡 Para detener el servidor: Ctrl+C"
echo "=================================================="
echo ""

# Ejecutar el servidor
./mvnw spring-boot:run
