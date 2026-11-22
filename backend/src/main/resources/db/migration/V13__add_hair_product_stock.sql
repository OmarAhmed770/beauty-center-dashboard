CREATE TABLE hair_product_stocks (
    id BIGSERIAL PRIMARY KEY,
    hair_item_id BIGINT NOT NULL,
    available_grams NUMERIC(19, 3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_hair_product_stocks_item UNIQUE (hair_item_id),
    CONSTRAINT fk_hair_product_stocks_item
        FOREIGN KEY (hair_item_id) REFERENCES catalog_items (id) ON DELETE RESTRICT,
    CONSTRAINT chk_hair_product_stocks_available_nonnegative CHECK (available_grams >= 0),
    CONSTRAINT chk_hair_product_stocks_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_hair_product_stocks_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE hair_stock_movements (
    id BIGSERIAL PRIMARY KEY,
    hair_item_id BIGINT NOT NULL,
    invoice_item_id BIGINT,
    refund_id BIGINT,
    movement_type VARCHAR(30) NOT NULL,
    event_key VARCHAR(160) NOT NULL,
    previous_grams NUMERIC(19, 3) NOT NULL,
    adjustment_grams NUMERIC(19, 3) NOT NULL,
    new_grams NUMERIC(19, 3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_hair_stock_movements_event_key UNIQUE (event_key),
    CONSTRAINT fk_hair_stock_movements_item
        FOREIGN KEY (hair_item_id) REFERENCES catalog_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_hair_stock_movements_invoice_item
        FOREIGN KEY (invoice_item_id) REFERENCES invoice_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_hair_stock_movements_refund
        FOREIGN KEY (refund_id) REFERENCES invoice_refunds (id) ON DELETE RESTRICT,
    CONSTRAINT chk_hair_stock_movements_type CHECK (
        movement_type IN ('INITIAL', 'MANUAL_ADJUSTMENT', 'SALE', 'CANCELLATION_RESTORE', 'REFUND_RESTORE')
    ),
    CONSTRAINT chk_hair_stock_movements_previous_nonnegative CHECK (previous_grams >= 0),
    CONSTRAINT chk_hair_stock_movements_new_nonnegative CHECK (new_grams >= 0),
    CONSTRAINT chk_hair_stock_movements_adjustment CHECK (
        movement_type = 'INITIAL' OR adjustment_grams <> 0
    ),
    CONSTRAINT chk_hair_stock_movements_reason_not_blank CHECK (length(trim(reason)) >= 1),
    CONSTRAINT chk_hair_stock_movements_created_by CHECK (length(trim(created_by)) >= 1)
);

CREATE INDEX idx_hair_stock_movements_item_created
    ON hair_stock_movements (hair_item_id, created_at DESC, id DESC);
CREATE INDEX idx_hair_stock_movements_invoice_item
    ON hair_stock_movements (invoice_item_id, movement_type);
