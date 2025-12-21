-- Indexes for tickets table
CREATE INDEX idx_tickets_uuid ON tickets(uuid);
CREATE INDEX idx_tickets_ticket_number ON tickets(ticket_number);
CREATE INDEX idx_tickets_status_queue_type ON tickets(status, queue_type);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_tickets_advisor_id ON tickets(advisor_id);

-- Indexes for advisors table
CREATE INDEX idx_advisors_status_queue_type ON advisors(status, queue_type);
CREATE INDEX idx_advisors_updated_at ON advisors(updated_at);

-- Indexes for messages table
CREATE INDEX idx_messages_ticket_id ON messages(ticket_id);
CREATE INDEX idx_messages_delivery_status ON messages(delivery_status);
CREATE INDEX idx_messages_created_at ON messages(created_at);