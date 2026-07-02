ALTER TABLE compra
    ADD COLUMN event_id VARCHAR(64);

CREATE INDEX idx_compra_event_id ON compra(event_id);
