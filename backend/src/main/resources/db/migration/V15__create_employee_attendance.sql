CREATE TABLE employee_attendances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    business_date DATE NOT NULL,
    attendance_status VARCHAR(20) NOT NULL,
    daily_wage_amount NUMERIC(19, 2) NOT NULL,
    daily_wage_transaction_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_employee_attendances_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_attendances_daily_wage
        FOREIGN KEY (daily_wage_transaction_id) REFERENCES employee_transactions (id) ON DELETE RESTRICT,
    CONSTRAINT uk_employee_attendances_employee_date UNIQUE (employee_id, business_date),
    CONSTRAINT uk_employee_attendances_daily_wage UNIQUE (daily_wage_transaction_id),
    CONSTRAINT chk_employee_attendances_status CHECK (attendance_status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT chk_employee_attendances_daily_wage_nonnegative CHECK (daily_wage_amount >= 0),
    CONSTRAINT chk_employee_attendances_created_by_not_blank CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_employee_attendances_updated_by_not_blank CHECK (length(trim(updated_by)) >= 1)
);

CREATE INDEX idx_employee_attendances_business_date
    ON employee_attendances (business_date, employee_id);
