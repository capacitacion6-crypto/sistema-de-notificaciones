-- Create mensaje table (Spanish version of messages)
CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    plantilla VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL,
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mensaje_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

-- Create index for performance
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);
CREATE INDEX idx_mensaje_estado_envio ON mensaje(estado_envio);
CREATE INDEX idx_mensaje_fecha_programada ON mensaje(fecha_programada);