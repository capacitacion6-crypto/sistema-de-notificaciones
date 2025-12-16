-- V2: Crear índices para optimizar performance

-- Índices en tabla ticket
CREATE INDEX idx_ticket_codigo_referencia ON ticket(codigo_referencia);
CREATE INDEX idx_ticket_numero ON ticket(numero);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);
CREATE INDEX idx_ticket_assigned_advisor ON ticket(assigned_advisor_id);

-- Índices en tabla mensaje
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
CREATE INDEX idx_mensaje_plantilla ON mensaje(plantilla);

-- Índices en tabla advisor
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_module_number ON advisor(module_number);
CREATE INDEX idx_advisor_assigned_count ON advisor(assigned_tickets_count);