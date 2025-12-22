# 🤖 Scripts de Automatización

Script final para automatizar el flujo completo de tickets del sistema.

## 📋 Prerrequisitos

1. **Aplicación corriendo**: `http://localhost:8080`
2. **Python 3** instalado
3. **Dependencias Python**:
   ```bash
   pip install -r requirements.txt
   ```

## 🚀 Script Principal

### `final-ticket-automation.py`
**El único script que necesitas usar**

- Crea 4 tickets (uno de cada tipo de cola)
- Los procesa automáticamente cada 30 segundos
- Completa todos los tickets hasta terminar
- Funciona de manera robusta y confiable

### Ejecución:

```bash
# Windows
run-final-automation.bat

# Manual
python final-ticket-automation.py
```

## 📊 Qué hace el script

1. **Configura** todos los asesores como disponibles
2. **Crea 4 tickets** (CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA)
3. **Procesa automáticamente** hasta completar todos:
   - Asigna tickets a asesores disponibles
   - Completa tickets asignados
   - Repite cada 30 segundos hasta terminar

## 📊 Salida del Script

```
[04:18:44] === INICIANDO AUTOMATIZACION DE FLUJO DE TICKETS ===
[04:18:44] Todos los asesores configurados como disponibles
[04:18:46] Creando 4 tickets...
[04:18:47] Ticket 1 creado: C071846 (CAJA)
[04:18:47] Ticket 2 creado: PB071847 (PERSONAL_BANKER)
[04:18:48] Ticket 3 creado: E071847 (EMPRESAS)
[04:18:48] Ticket 4 creado: G071848 (GERENCIA)
[04:18:48] Creados 4 tickets
[04:18:51] Estado inicial: 4 esperando, 0 en progreso, 30 completados
[04:18:51] 
--- CICLO 1 ---
[04:19:06] Procesados 199 tickets en este ciclo
[04:19:09] Estado: 0 esperando, 0 en progreso, 34 completados
```

## ⏹️ Detener el Script

Presiona `Ctrl+C` para detener la automatización.

## 🧪 Scripts de Prueba

- `test-flujo-funcional.sh` - Pruebas funcionales completas
- `test-flujo-funcional.bat` - Versión Windows de las pruebas

## ✅ Resultado

El script procesa automáticamente todos los tickets hasta completarlos, demostrando el flujo completo del sistema ticketero.