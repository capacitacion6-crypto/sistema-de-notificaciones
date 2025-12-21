-- Create advisors table
CREATE TABLE advisors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    module_number INTEGER NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Create tickets table
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL,
    ticket_number VARCHAR(20) UNIQUE NOT NULL,
    customer_rut VARCHAR(12) NOT NULL,
    customer_phone VARCHAR(15),
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    queue_position INTEGER,
    estimated_wait_minutes INTEGER,
    advisor_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    assigned_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_ticket_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id)
);

-- Create messages table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP,
    delivery_status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_message_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);