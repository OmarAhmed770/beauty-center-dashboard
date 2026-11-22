CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL,
    business_date DATE NOT NULL,
    customer_name VARCHAR(150),
    customer_phone VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    subtotal NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    discount_total NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    final_total NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    refunded_total NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    notes VARCHAR(2000),
    completed_by VARCHAR(100),
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_by VARCHAR(100),
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    cancellation_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT chk_invoices_number_not_blank CHECK (length(trim(invoice_number)) >= 1),
    CONSTRAINT chk_invoices_status CHECK (
        status IN ('DRAFT', 'COMPLETED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED')
    ),
    CONSTRAINT chk_invoices_totals_nonnegative CHECK (
        subtotal >= 0 AND discount_total >= 0 AND final_total >= 0 AND refunded_total >= 0
    ),
    CONSTRAINT chk_invoices_discount_limit CHECK (discount_total <= subtotal),
    CONSTRAINT chk_invoices_refund_limit CHECK (refunded_total <= final_total),
    CONSTRAINT chk_invoices_customer_name CHECK (
        customer_name IS NULL OR length(trim(customer_name)) >= 1
    ),
    CONSTRAINT chk_invoices_customer_phone CHECK (
        customer_phone IS NULL OR length(trim(customer_phone)) >= 1
    ),
    CONSTRAINT chk_invoices_notes CHECK (notes IS NULL OR length(trim(notes)) >= 1),
    CONSTRAINT chk_invoices_completion_pair CHECK (
        (completed_by IS NULL AND completed_at IS NULL)
        OR (completed_by IS NOT NULL AND length(trim(completed_by)) >= 1 AND completed_at IS NOT NULL)
    ),
    CONSTRAINT chk_invoices_completed_state CHECK (
        status IN ('DRAFT') OR status = 'CANCELLED'
        OR (completed_by IS NOT NULL AND completed_at IS NOT NULL)
    ),
    CONSTRAINT chk_invoices_cancellation_state CHECK (
        (status <> 'CANCELLED'
            AND cancelled_by IS NULL
            AND cancelled_at IS NULL
            AND cancellation_reason IS NULL)
        OR
        (status = 'CANCELLED'
            AND cancelled_by IS NOT NULL
            AND length(trim(cancelled_by)) >= 1
            AND cancelled_at IS NOT NULL
            AND cancellation_reason IS NOT NULL
            AND length(trim(cancellation_reason)) >= 1)
    ),
    CONSTRAINT chk_invoices_refund_state CHECK (
        (status IN ('DRAFT', 'COMPLETED', 'CANCELLED') AND refunded_total = 0)
        OR (status = 'PARTIALLY_REFUNDED' AND refunded_total > 0 AND refunded_total < final_total)
        OR (status = 'REFUNDED' AND refunded_total = final_total)
    ),
    CONSTRAINT chk_invoices_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_invoices_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    catalog_item_id BIGINT,
    hair_configuration_id BIGINT,
    item_code_snapshot VARCHAR(50) NOT NULL,
    item_name_arabic_snapshot VARCHAR(150) NOT NULL,
    item_name_english_snapshot VARCHAR(150) NOT NULL,
    item_type_snapshot VARCHAR(30) NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_original_selling_price NUMERIC(19, 2) NOT NULL,
    unit_cost_snapshot NUMERIC(19, 2) NOT NULL,
    installation_fee_snapshot NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    discount_id BIGINT,
    discount_name_snapshot VARCHAR(150),
    discount_type_snapshot VARCHAR(20),
    discount_value_snapshot NUMERIC(19, 2),
    discount_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    line_subtotal NUMERIC(19, 2) NOT NULL,
    final_line_total NUMERIC(19, 2) NOT NULL,
    employee_id BIGINT,
    employee_name_snapshot VARCHAR(150),
    price_override_original_amount NUMERIC(19, 2),
    price_override_amount NUMERIC(19, 2),
    price_override_reason VARCHAR(500),
    price_overridden_by VARCHAR(100),
    price_overridden_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_items_catalog_item
        FOREIGN KEY (catalog_item_id) REFERENCES catalog_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_items_hair_configuration
        FOREIGN KEY (hair_configuration_id) REFERENCES hair_configurations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_items_discount
        FOREIGN KEY (discount_id) REFERENCES discounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_items_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT chk_invoice_items_code_not_blank CHECK (length(trim(item_code_snapshot)) >= 1),
    CONSTRAINT chk_invoice_items_arabic_not_blank CHECK (length(trim(item_name_arabic_snapshot)) >= 1),
    CONSTRAINT chk_invoice_items_english_not_blank CHECK (length(trim(item_name_english_snapshot)) >= 1),
    CONSTRAINT chk_invoice_items_type CHECK (
        item_type_snapshot IN ('SERVICE', 'PRODUCT', 'HAIR_PRODUCT', 'HAIR_INSTALLATION')
    ),
    CONSTRAINT chk_invoice_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_invoice_items_money_nonnegative CHECK (
        unit_original_selling_price >= 0
        AND unit_cost_snapshot >= 0
        AND installation_fee_snapshot >= 0
        AND discount_amount >= 0
        AND line_subtotal >= 0
        AND final_line_total >= 0
    ),
    CONSTRAINT chk_invoice_items_discount_math CHECK (discount_amount <= line_subtotal),
    CONSTRAINT chk_invoice_items_discount_snapshot CHECK (
        (discount_id IS NULL
            AND discount_name_snapshot IS NULL
            AND discount_type_snapshot IS NULL
            AND discount_value_snapshot IS NULL
            AND discount_amount = 0)
        OR
        (discount_id IS NOT NULL
            AND discount_name_snapshot IS NOT NULL
            AND length(trim(discount_name_snapshot)) >= 1
            AND discount_type_snapshot IN ('PERCENTAGE', 'FIXED_AMOUNT')
            AND discount_value_snapshot IS NOT NULL
            AND discount_value_snapshot >= 0)
    ),
    CONSTRAINT chk_invoice_items_employee_snapshot CHECK (
        (employee_id IS NULL AND employee_name_snapshot IS NULL)
        OR (employee_id IS NOT NULL AND employee_name_snapshot IS NOT NULL
            AND length(trim(employee_name_snapshot)) >= 1)
    ),
    CONSTRAINT chk_invoice_items_override_snapshot CHECK (
        (price_override_amount IS NULL
            AND price_override_original_amount IS NULL
            AND price_override_reason IS NULL
            AND price_overridden_by IS NULL
            AND price_overridden_at IS NULL)
        OR
        (price_override_amount IS NOT NULL
            AND price_override_amount >= 0
            AND price_override_original_amount IS NOT NULL
            AND price_override_original_amount >= 0
            AND price_override_amount <> price_override_original_amount
            AND price_override_reason IS NOT NULL
            AND length(trim(price_override_reason)) >= 1
            AND price_overridden_by IS NOT NULL
            AND length(trim(price_overridden_by)) >= 1
            AND price_overridden_at IS NOT NULL
            AND final_line_total = price_override_amount)
    ),
    CONSTRAINT chk_invoice_items_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_invoice_items_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE invoice_payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    payment_method_code_snapshot VARCHAR(50) NOT NULL,
    payment_method_name_arabic_snapshot VARCHAR(150) NOT NULL,
    payment_method_name_english_snapshot VARCHAR(150) NOT NULL,
    payment_method_type_snapshot VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reference_number VARCHAR(200),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_invoice_payments_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_payments_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT uk_invoice_payments_method UNIQUE (invoice_id, payment_method_id),
    CONSTRAINT chk_invoice_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_invoice_payments_code CHECK (length(trim(payment_method_code_snapshot)) >= 1),
    CONSTRAINT chk_invoice_payments_arabic CHECK (length(trim(payment_method_name_arabic_snapshot)) >= 1),
    CONSTRAINT chk_invoice_payments_english CHECK (length(trim(payment_method_name_english_snapshot)) >= 1),
    CONSTRAINT chk_invoice_payments_type CHECK (
        payment_method_type_snapshot IN ('CASH', 'CARD', 'WALLET', 'OTHER')
    ),
    CONSTRAINT chk_invoice_payments_reference CHECK (
        reference_number IS NULL OR length(trim(reference_number)) >= 1
    ),
    CONSTRAINT chk_invoice_payments_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_invoice_payments_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE invoice_refunds (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    business_date DATE NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_invoice_refunds_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE RESTRICT,
    CONSTRAINT chk_invoice_refunds_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_invoice_refunds_reason CHECK (length(trim(reason)) >= 1),
    CONSTRAINT chk_invoice_refunds_status CHECK (status = 'COMPLETED'),
    CONSTRAINT chk_invoice_refunds_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_invoice_refunds_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE invoice_refund_payments (
    id BIGSERIAL PRIMARY KEY,
    refund_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    payment_method_code_snapshot VARCHAR(50) NOT NULL,
    payment_method_name_arabic_snapshot VARCHAR(150) NOT NULL,
    payment_method_name_english_snapshot VARCHAR(150) NOT NULL,
    payment_method_type_snapshot VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reference_number VARCHAR(200),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_refund_payments_refund
        FOREIGN KEY (refund_id) REFERENCES invoice_refunds (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_payments_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT uk_refund_payments_method UNIQUE (refund_id, payment_method_id),
    CONSTRAINT chk_refund_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_refund_payments_code CHECK (length(trim(payment_method_code_snapshot)) >= 1),
    CONSTRAINT chk_refund_payments_arabic CHECK (length(trim(payment_method_name_arabic_snapshot)) >= 1),
    CONSTRAINT chk_refund_payments_english CHECK (length(trim(payment_method_name_english_snapshot)) >= 1),
    CONSTRAINT chk_refund_payments_type CHECK (
        payment_method_type_snapshot IN ('CASH', 'CARD', 'WALLET', 'OTHER')
    ),
    CONSTRAINT chk_refund_payments_reference CHECK (
        reference_number IS NULL OR length(trim(reference_number)) >= 1
    ),
    CONSTRAINT chk_refund_payments_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_refund_payments_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    business_date DATE,
    actor VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    details VARCHAR(4000),
    CONSTRAINT chk_audit_logs_action CHECK (length(trim(action)) >= 1),
    CONSTRAINT chk_audit_logs_target_type CHECK (length(trim(target_type)) >= 1),
    CONSTRAINT chk_audit_logs_target_id CHECK (length(trim(target_id)) >= 1),
    CONSTRAINT chk_audit_logs_actor CHECK (length(trim(actor)) >= 1),
    CONSTRAINT chk_audit_logs_details CHECK (details IS NULL OR length(trim(details)) >= 1)
);

CREATE INDEX idx_invoices_business_date_status
    ON invoices (business_date, status);
CREATE INDEX idx_invoices_customer_phone
    ON invoices (customer_phone);
CREATE INDEX idx_invoice_items_invoice
    ON invoice_items (invoice_id, id);
CREATE INDEX idx_invoice_items_catalog
    ON invoice_items (catalog_item_id);
CREATE INDEX idx_invoice_items_employee
    ON invoice_items (employee_id, invoice_id);
CREATE INDEX idx_invoice_payments_invoice
    ON invoice_payments (invoice_id, id);
CREATE INDEX idx_invoice_payments_method
    ON invoice_payments (payment_method_id, invoice_id);
CREATE INDEX idx_invoice_refunds_invoice_date
    ON invoice_refunds (invoice_id, business_date, id);
CREATE INDEX idx_refund_payments_refund
    ON invoice_refund_payments (refund_id, id);
CREATE INDEX idx_audit_logs_target
    ON audit_logs (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_business_date
    ON audit_logs (business_date, occurred_at DESC);
