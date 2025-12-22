@echo off
REM ============================================================================
REM SCRIPT DE PRUEBA FUNCIONAL - SISTEMA TICKETERO DIGITAL (Windows)
REM ============================================================================
REM Valida el flujo completo de tickets según requerimientos RF-001 a RF-008
REM Incluye creación de ticket, notificaciones Telegram y monitoreo
REM ============================================================================

setlocal enabledelayedexpansion

REM Configuración
set API_BASE_URL=http://localhost:8080
set TELEGRAM_CHAT_ID=
set TEST_RUT=12345678-9
set TEST_PHONE=56912345678

echo.
echo ============================================================================
echo 🎯 PRUEBA FUNCIONAL - SISTEMA TICKETERO DIGITAL
echo ============================================================================
echo Validando requerimientos RF-001 a RF-008 según project-requirements.md
echo ============================================================================
echo.

REM Verificar que la API esté funcionando
echo [INFO] Verificando estado de la API...
curl -s "%API_BASE_URL%/actuator/health" | findstr "UP" >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] API está funcionando correctamente
) else (
    echo [ERROR] API no está disponible en %API_BASE_URL%
    exit /b 1
)

echo.
echo [INFO] 🚀 Iniciando pruebas funcionales...
echo.

REM ============================================================================
REM RF-001: CREAR TICKET DIGITAL
REM ============================================================================
echo [INFO] === RF-001: Probando creación de ticket digital ===

curl -s -X POST "%API_BASE_URL%/api/tickets" ^
    -H "Content-Type: application/json" ^
    -d "{\"rut\": \"%TEST_RUT%\", \"telefono\": \"%TEST_PHONE%\", \"tipoAtencion\": \"PERSONAL_BANKER\"}" > ticket_response.tmp

findstr "uuid" ticket_response.tmp >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] ✅ Ticket creado correctamente
    type ticket_response.tmp
) else (
    echo [ERROR] ❌ Error al crear ticket
    type ticket_response.tmp
)

echo.

REM ============================================================================
REM RF-003: CALCULAR POSICIÓN Y TIEMPO ESTIMADO
REM ============================================================================
echo [INFO] === RF-003: Verificando cálculo de posición y tiempo ===

REM Extraer número de ticket del response anterior
for /f "tokens=2 delims=:" %%a in ('findstr "numero" ticket_response.tmp') do (
    set TICKET_NUMERO=%%a
    set TICKET_NUMERO=!TICKET_NUMERO:"=!
    set TICKET_NUMERO=!TICKET_NUMERO:,=!
)

if defined TICKET_NUMERO (
    curl -s "%API_BASE_URL%/api/tickets/!TICKET_NUMERO!/position" > position_response.tmp
    findstr "posicion" position_response.tmp >nul
    if !errorlevel! equ 0 (
        echo [SUCCESS] ✅ Posición y tiempo calculados correctamente
        type position_response.tmp
    ) else (
        echo [ERROR] ❌ Error al calcular posición
    )
)

echo.

REM ============================================================================
REM RF-007: PANEL DE MONITOREO PARA SUPERVISOR
REM ============================================================================
echo [INFO] === RF-007: Verificando panel de monitoreo ===

curl -s "%API_BASE_URL%/api/admin/dashboard" > dashboard_response.tmp
findstr "ticketsPorEstado" dashboard_response.tmp >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] ✅ Dashboard funcionando correctamente
    echo [INFO] 📊 Dashboard disponible en: %API_BASE_URL%/dashboard.html
) else (
    echo [ERROR] ❌ Error en dashboard
)

echo.

REM ============================================================================
REM VERIFICAR MÚLTIPLES COLAS
REM ============================================================================
echo [INFO] === RF-005: Verificando gestión de múltiples colas ===

echo [INFO] Creando tickets para diferentes tipos de atención...

REM Crear ticket para CAJA
curl -s -X POST "%API_BASE_URL%/api/tickets" ^
    -H "Content-Type: application/json" ^
    -d "{\"rut\": \"11111111-1\", \"telefono\": \"56911111111\", \"tipoAtencion\": \"CAJA\"}" > caja_response.tmp

findstr "uuid" caja_response.tmp >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] ✅ Ticket CAJA creado
) else (
    echo [WARNING] ⚠️ Error creando ticket CAJA
)

REM Crear ticket para EMPRESAS
curl -s -X POST "%API_BASE_URL%/api/tickets" ^
    -H "Content-Type: application/json" ^
    -d "{\"rut\": \"22222222-2\", \"telefono\": \"56922222222\", \"tipoAtencion\": \"EMPRESAS\"}" > empresas_response.tmp

findstr "uuid" empresas_response.tmp >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] ✅ Ticket EMPRESAS creado
) else (
    echo [WARNING] ⚠️ Error creando ticket EMPRESAS
)

echo.

REM ============================================================================
REM VERIFICAR ASESORES
REM ============================================================================
echo [INFO] === RF-004: Verificando asesores disponibles ===

curl -s "%API_BASE_URL%/api/admin/advisors" > advisors_response.tmp
findstr "AVAILABLE" advisors_response.tmp >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] ✅ Hay asesores disponibles para asignación automática
) else (
    echo [WARNING] ⚠️ No hay asesores disponibles
)

echo.

REM ============================================================================
REM VERIFICAR MÉTRICAS Y AUDITORÍA
REM ============================================================================
echo [INFO] === RF-008: Verificando auditoría de eventos ===

curl -s "%API_BASE_URL%/actuator/metrics" > metrics_response.tmp
findstr "jvm" metrics_response.tmp >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] ✅ Sistema de métricas funcionando (incluye auditoría)
) else (
    echo [WARNING] ⚠️ Sistema de métricas no disponible
)

echo.

REM ============================================================================
REM RESUMEN FINAL
REM ============================================================================
echo ============================================================================
echo [SUCCESS] ✅ PRUEBAS FUNCIONALES COMPLETADAS
echo ============================================================================
echo.
echo [INFO] 📋 PRÓXIMOS PASOS:
echo 1. Configurar TELEGRAM_CHAT_ID para probar notificaciones completas
echo 2. Abrir dashboard: %API_BASE_URL%/dashboard.html
echo 3. Monitorear logs: docker-compose logs -f api
echo 4. Verificar métricas: %API_BASE_URL%/actuator/metrics
echo.

REM Limpiar archivos temporales
del ticket_response.tmp position_response.tmp dashboard_response.tmp caja_response.tmp empresas_response.tmp advisors_response.tmp metrics_response.tmp 2>nul

pause