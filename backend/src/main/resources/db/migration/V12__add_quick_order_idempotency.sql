ALTER TABLE invoices
    ADD COLUMN client_request_id VARCHAR(100);

ALTER TABLE invoices
    ADD CONSTRAINT uk_invoices_client_request_id UNIQUE (client_request_id);

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_client_request_id CHECK (
        client_request_id IS NULL OR length(trim(client_request_id)) >= 1
    );

CREATE INDEX idx_invoices_created_by_business_date
    ON invoices (created_by, business_date, id DESC);
