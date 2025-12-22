#!/usr/bin/env python3
"""
Script final: Crea 4 tickets y los procesa cada 30 segundos hasta completarlos
"""
import requests
import time
from datetime import datetime

BASE_URL = "http://localhost:8080"

def log(message):
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {message}")

def create_4_tickets():
    """Crear 4 tickets de diferentes tipos"""
    tickets_data = [
        {"customerRut": "12345678-9", "customerPhone": "+56912345678", "queueType": "CAJA"},
        {"customerRut": "87654321-0", "customerPhone": "+56987654321", "queueType": "PERSONAL_BANKER"},
        {"customerRut": "11111111-1", "customerPhone": "+56911111111", "queueType": "EMPRESAS"},
        {"customerRut": "22222222-2", "customerPhone": "+56922222222", "queueType": "GERENCIA"}
    ]
    
    created = 0
    for i, ticket_data in enumerate(tickets_data, 1):
        try:
            response = requests.post(f"{BASE_URL}/api/tickets", json=ticket_data)
            if response.status_code == 201:
                ticket = response.json()
                log(f"Ticket {i} creado: {ticket['ticketNumber']} ({ticket_data['queueType']})")
                created += 1
            else:
                log(f"Error creando ticket {i}: {response.status_code}")
        except Exception as e:
            log(f"Error creando ticket {i}: {e}")
    
    return created

def setup_advisors():
    """Configurar todos los asesores como disponibles"""
    for advisor_id in range(1, 7):
        requests.put(f"{BASE_URL}/api/admin/advisors/{advisor_id}/status?status=AVAILABLE")
    log("Todos los asesores configurados como disponibles")

def get_status():
    """Obtener estado actual del sistema"""
    try:
        response = requests.get(f"{BASE_URL}/api/admin/dashboard")
        if response.status_code == 200:
            data = response.json()
            summary = data['summary']
            return summary['ticketsWaiting'], summary['ticketsInProgress'], summary['ticketsCompleted']
        return 0, 0, 0
    except:
        return 0, 0, 0

def process_cycle():
    """Procesar un ciclo completo: asignar y completar"""
    # 1. Procesar asignaciones múltiples veces
    for i in range(5):
        requests.post(f"{BASE_URL}/api/admin/assignments/process")
        time.sleep(1)
    
    # 2. Completar tickets por fuerza bruta
    completed = 0
    for ticket_id in range(1, 200):  # Rango amplio
        try:
            response = requests.post(f"{BASE_URL}/api/admin/tickets/{ticket_id}/complete")
            if response.status_code == 200:
                completed += 1
            elif response.status_code == 404:
                break  # No más tickets
        except:
            pass
    
    return completed

def main():
    log("=== INICIANDO AUTOMATIZACION DE FLUJO DE TICKETS ===")
    
    # 1. Configurar asesores
    setup_advisors()
    time.sleep(2)
    
    # 2. Crear 4 tickets
    log("Creando 4 tickets...")
    created_tickets = create_4_tickets()
    if created_tickets == 0:
        log("No se pudieron crear tickets. Terminando.")
        return
    
    log(f"Creados {created_tickets} tickets")
    time.sleep(3)
    
    # 3. Estado inicial
    waiting, in_progress, completed = get_status()
    log(f"Estado inicial: {waiting} esperando, {in_progress} en progreso, {completed} completados")
    
    if waiting == 0:
        log("No hay tickets para procesar")
        return
    
    # 4. Procesar en ciclos cada 30 segundos
    cycle = 1
    max_cycles = 10
    
    while cycle <= max_cycles:
        log(f"\n--- CICLO {cycle} ---")
        
        # Procesar tickets
        completed_this_cycle = process_cycle()
        log(f"Procesados {completed_this_cycle} tickets en este ciclo")
        
        time.sleep(3)
        
        # Ver estado
        waiting, in_progress, completed_total = get_status()
        log(f"Estado: {waiting} esperando, {in_progress} en progreso, {completed_total} completados")
        
        # Si no hay tickets pendientes, terminar
        if waiting == 0 and in_progress == 0:
            log(f"\n🎉 TODOS LOS TICKETS PROCESADOS!")
            log(f"Total completados: {completed_total}")
            break
        
        if cycle < max_cycles:
            log("Esperando 30 segundos para el siguiente ciclo...")
            time.sleep(30)
        
        cycle += 1
    
    # Estado final
    waiting, in_progress, completed_total = get_status()
    log(f"\n=== ESTADO FINAL ===")
    log(f"Esperando: {waiting}, En progreso: {in_progress}, Completados: {completed_total}")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        log("\nScript interrumpido por el usuario")
    except Exception as e:
        log(f"Error: {e}")