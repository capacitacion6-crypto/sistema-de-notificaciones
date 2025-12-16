-- V1: Crear tablas principales del sistema

-- Tabla de asesores
CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    module_number INTEGER NOT NULL,
    assigned_tickets_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Tabla de tickets
CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia UUID UNIQUE NOT NULL,
    numero VARCHAR(10) UNIQUE NOT NULL,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    branch_office VARCHAR(100) NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    position_in_queue INTEGER,
    estimated_wait_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    assigned_advisor_id BIGINT,
    assigned_module_number INTEGER,
    CONSTRAINT fk_ticket_advisor FOREIGN KEY (assigned_advisor_id) 
        REFERENCES advisor(id) ON DELETE SET NULL
);

-- Tabla de mensajes
CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    plantilla VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mensaje_ticket FOREIGN KEY (ticket_id) 
        REFERENCES ticket(id) ON DELETE CASCADE
);