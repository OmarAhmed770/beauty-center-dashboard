CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    position VARCHAR(100) NOT NULL,
    default_daily_wage NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_employees_name_not_blank CHECK (length(trim(name)) >= 1),
    CONSTRAINT chk_employees_phone_not_blank CHECK (phone IS NULL OR length(trim(phone)) >= 1),
    CONSTRAINT chk_employees_position_not_blank CHECK (length(trim(position)) >= 1),
    CONSTRAINT chk_employees_default_daily_wage_nonnegative CHECK (default_daily_wage >= 0),
    CONSTRAINT chk_employees_created_by_not_blank CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_employees_updated_by_not_blank CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE employee_transactions (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    business_date DATE NOT NULL,
    payment_method VARCHAR(100),
    notes VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_employee_transactions_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_transactions_type CHECK (
        transaction_type IN ('DAILY_WAGE', 'ADVANCE', 'BONUS', 'DEDUCTION', 'COMMISSION', 'PAYMENT')
    ),
    CONSTRAINT chk_employee_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_employee_transactions_payment_method_not_blank CHECK (
        payment_method IS NULL OR length(trim(payment_method)) >= 1
    ),
    CONSTRAINT chk_employee_transactions_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT chk_employee_transactions_cancellation_state CHECK (
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
    CONSTRAINT chk_employee_transactions_created_by_not_blank CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_employee_transactions_updated_by_not_blank CHECK (length(trim(updated_by)) >= 1)
);

CREATE INDEX idx_employees_name ON employees (name);
CREATE INDEX idx_employees_active_name ON employees (active, name);
CREATE INDEX idx_employee_transactions_employee_date
    ON employee_transactions (employee_id, business_date);
CREATE INDEX idx_employee_transactions_employee_status_date
    ON employee_transactions (employee_id, status, business_date);
CREATE INDEX idx_employee_transactions_type_date
    ON employee_transactions (transaction_type, business_date);
CREATE INDEX idx_employee_transactions_business_date
    ON employee_transactions (business_date);
