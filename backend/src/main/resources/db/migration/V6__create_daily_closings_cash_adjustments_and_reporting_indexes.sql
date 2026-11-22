CREATE TABLE cash_adjustments (
    id BIGSERIAL PRIMARY KEY,
    business_date DATE NOT NULL,
    adjustment_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_cash_adjustments_type CHECK (adjustment_type IN ('CASH_IN', 'CASH_OUT')),
    CONSTRAINT chk_cash_adjustments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_cash_adjustments_reason CHECK (length(trim(reason)) >= 1),
    CONSTRAINT chk_cash_adjustments_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT chk_cash_adjustments_cancellation CHECK (
        (status = 'ACTIVE'
            AND cancelled_at IS NULL
            AND cancelled_by IS NULL
            AND cancellation_reason IS NULL)
        OR
        (status = 'CANCELLED'
            AND cancelled_at IS NOT NULL
            AND cancelled_by IS NOT NULL
            AND length(trim(cancelled_by)) >= 1
            AND cancellation_reason IS NOT NULL
            AND length(trim(cancellation_reason)) >= 1)
    ),
    CONSTRAINT chk_cash_adjustments_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_cash_adjustments_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE daily_closings (
    id BIGSERIAL PRIMARY KEY,
    business_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    gross_sales NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    discounts NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    refunds NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    net_sales NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    completed_invoice_count BIGINT NOT NULL DEFAULT 0,
    expense_total NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    daily_wages NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    bonuses NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    commissions NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    employee_cost NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    advances NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    employee_payments NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    cost_of_sold_items NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    net_profit NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    cash_received NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    cash_expenses NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    cash_employee_outflows NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    cash_refunds NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    cash_adjustments NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    expected_cash NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    actual_cash NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    cash_difference NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    notes VARCHAR(2000),
    closed_by VARCHAR(100),
    closed_at TIMESTAMP WITHOUT TIME ZONE,
    reopened_by VARCHAR(100),
    reopened_at TIMESTAMP WITHOUT TIME ZONE,
    reopening_reason VARCHAR(500),
    reopen_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_daily_closings_business_date UNIQUE (business_date),
    CONSTRAINT chk_daily_closings_status CHECK (status IN ('OPEN', 'CLOSED', 'REOPENED')),
    CONSTRAINT chk_daily_closings_nonnegative CHECK (
        gross_sales >= 0
        AND discounts >= 0
        AND refunds >= 0
        AND completed_invoice_count >= 0
        AND expense_total >= 0
        AND daily_wages >= 0
        AND bonuses >= 0
        AND commissions >= 0
        AND employee_cost >= 0
        AND advances >= 0
        AND employee_payments >= 0
        AND cost_of_sold_items >= 0
        AND cash_received >= 0
        AND cash_expenses >= 0
        AND cash_employee_outflows >= 0
        AND cash_refunds >= 0
        AND actual_cash >= 0
        AND reopen_count >= 0
    ),
    CONSTRAINT chk_daily_closings_sales_math CHECK (net_sales = gross_sales - discounts - refunds),
    CONSTRAINT chk_daily_closings_employee_math CHECK (
        employee_cost = daily_wages + bonuses + commissions
    ),
    CONSTRAINT chk_daily_closings_profit_math CHECK (
        net_profit = net_sales - cost_of_sold_items - expense_total - employee_cost
    ),
    CONSTRAINT chk_daily_closings_cash_math CHECK (
        expected_cash = cash_received - cash_expenses - cash_employee_outflows - cash_refunds + cash_adjustments
        AND cash_difference = actual_cash - expected_cash
    ),
    CONSTRAINT chk_daily_closings_notes CHECK (notes IS NULL OR length(trim(notes)) >= 1),
    CONSTRAINT chk_daily_closings_closed_pair CHECK (
        (closed_by IS NULL AND closed_at IS NULL)
        OR (closed_by IS NOT NULL AND length(trim(closed_by)) >= 1 AND closed_at IS NOT NULL)
    ),
    CONSTRAINT chk_daily_closings_closed_state CHECK (
        status = 'OPEN' OR (closed_by IS NOT NULL AND closed_at IS NOT NULL)
    ),
    CONSTRAINT chk_daily_closings_reopened_pair CHECK (
        (reopened_by IS NULL AND reopened_at IS NULL AND reopening_reason IS NULL AND reopen_count = 0)
        OR
        (reopened_by IS NOT NULL
            AND length(trim(reopened_by)) >= 1
            AND reopened_at IS NOT NULL
            AND reopening_reason IS NOT NULL
            AND length(trim(reopening_reason)) >= 1
            AND reopen_count > 0)
    ),
    CONSTRAINT chk_daily_closings_reopened_state CHECK (
        status <> 'REOPENED' OR reopened_at IS NOT NULL
    ),
    CONSTRAINT chk_daily_closings_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_daily_closings_updated_by CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE daily_closing_payment_totals (
    id BIGSERIAL PRIMARY KEY,
    closing_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    payment_method_code_snapshot VARCHAR(50) NOT NULL,
    payment_method_name_arabic_snapshot VARCHAR(150) NOT NULL,
    payment_method_name_english_snapshot VARCHAR(150) NOT NULL,
    payment_method_type_snapshot VARCHAR(20) NOT NULL,
    received_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    refunded_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    net_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_closing_payment_totals_closing
        FOREIGN KEY (closing_id) REFERENCES daily_closings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_closing_payment_totals_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT uk_closing_payment_totals_method UNIQUE (closing_id, payment_method_id),
    CONSTRAINT chk_closing_payment_totals_code CHECK (length(trim(payment_method_code_snapshot)) >= 1),
    CONSTRAINT chk_closing_payment_totals_arabic CHECK (length(trim(payment_method_name_arabic_snapshot)) >= 1),
    CONSTRAINT chk_closing_payment_totals_english CHECK (length(trim(payment_method_name_english_snapshot)) >= 1),
    CONSTRAINT chk_closing_payment_totals_type CHECK (
        payment_method_type_snapshot IN ('CASH', 'CARD', 'WALLET', 'OTHER')
    ),
    CONSTRAINT chk_closing_payment_totals_nonnegative CHECK (
        received_amount >= 0 AND refunded_amount >= 0
    ),
    CONSTRAINT chk_closing_payment_totals_math CHECK (
        net_amount = received_amount - refunded_amount
    )
);

CREATE TABLE daily_closing_expense_totals (
    id BIGSERIAL PRIMARY KEY,
    closing_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_name_arabic_snapshot VARCHAR(150) NOT NULL,
    category_name_english_snapshot VARCHAR(150) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    expense_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_closing_expense_totals_closing
        FOREIGN KEY (closing_id) REFERENCES daily_closings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_closing_expense_totals_category
        FOREIGN KEY (category_id) REFERENCES expense_categories (id) ON DELETE RESTRICT,
    CONSTRAINT uk_closing_expense_totals_category UNIQUE (closing_id, category_id),
    CONSTRAINT chk_closing_expense_totals_arabic CHECK (length(trim(category_name_arabic_snapshot)) >= 1),
    CONSTRAINT chk_closing_expense_totals_english CHECK (length(trim(category_name_english_snapshot)) >= 1),
    CONSTRAINT chk_closing_expense_totals_nonnegative CHECK (amount >= 0 AND expense_count >= 0)
);

CREATE INDEX idx_cash_adjustments_date_status
    ON cash_adjustments (business_date, status);
CREATE INDEX idx_daily_closings_status_date
    ON daily_closings (status, business_date DESC);
CREATE INDEX idx_closing_payment_totals_closing
    ON daily_closing_payment_totals (closing_id, payment_method_code_snapshot);
CREATE INDEX idx_closing_expense_totals_closing
    ON daily_closing_expense_totals (closing_id, category_name_english_snapshot);
CREATE INDEX idx_invoice_refunds_business_date
    ON invoice_refunds (business_date, status);
CREATE INDEX idx_invoice_items_type_invoice
    ON invoice_items (item_type_snapshot, invoice_id);
CREATE INDEX idx_employee_transactions_status_date_type
    ON employee_transactions (status, business_date, transaction_type);
CREATE INDEX idx_employee_transactions_cash_reporting
    ON employee_transactions (status, business_date, payment_method_type, transaction_type);
CREATE INDEX idx_audit_logs_action_time
    ON audit_logs (action, occurred_at DESC);
CREATE INDEX idx_audit_logs_actor_time
    ON audit_logs (actor, occurred_at DESC);
