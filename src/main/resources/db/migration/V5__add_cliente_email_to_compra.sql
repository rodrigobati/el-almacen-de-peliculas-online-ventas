ALTER TABLE compra
    ADD COLUMN cliente_email VARCHAR(254);

UPDATE compra
SET cliente_email = cliente_id
WHERE cliente_email IS NULL
  AND cliente_id LIKE '%@%';