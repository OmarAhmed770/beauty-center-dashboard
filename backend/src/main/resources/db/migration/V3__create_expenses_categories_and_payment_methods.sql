CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    display_name_arabic VARCHAR(150) NOT NULL,
    display_name_english VARCHAR(150) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    requires_reference_number BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_payment_methods_code UNIQUE (code),
    CONSTRAINT chk_payment_methods_code_not_blank CHECK (length(trim(code)) >= 1),
    CONSTRAINT chk_payment_methods_code_normalized CHECK (code = upper(trim(code))),
    CONSTRAINT chk_payment_methods_arabic_not_blank CHECK (length(trim(display_name_arabic)) >= 1),
    CONSTRAINT chk_payment_methods_english_not_blank CHECK (length(trim(display_name_english)) >= 1),
    CONSTRAINT chk_payment_methods_type CHECK (payment_type IN ('CASH', 'CARD', 'WALLET', 'OTHER')),
    CONSTRAINT chk_payment_methods_sort_order CHECK (sort_order >= 0),
    CONSTRAINT chk_payment_methods_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_payment_methods_updated_by CHECK (length(trim(updated_by)) >= 1)
);

INSERT INTO payment_methods (
    code, display_name_arabic, display_name_english, payment_type,
    active, requires_reference_number, sort_order
) VALUES
    ('CASH', 'نقدي', 'Cash', 'CASH', TRUE, FALSE, 10),
    ('VISA', 'فيزا', 'Visa', 'CARD', TRUE, FALSE, 20),
    ('FAWRY', 'فوري', 'Fawry', 'OTHER', TRUE, TRUE, 30),
    ('INSTAPAY', 'إنستاباي', 'InstaPay', 'WALLET', TRUE, TRUE, 40),
    ('VODAFONE_CASH', 'فودافون كاش', 'Vodafone Cash', 'WALLET', TRUE, TRUE, 50);

CREATE TABLE expense_categories (
    id BIGSERIAL PRIMARY KEY,
    name_arabic VARCHAR(150) NOT NULL,
    name_english VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_expense_categories_name_arabic UNIQUE (name_arabic),
    CONSTRAINT uk_expense_categories_name_english UNIQUE (name_english),
    CONSTRAINT chk_expense_categories_arabic_not_blank CHECK (length(trim(name_arabic)) >= 1),
    CONSTRAINT chk_expense_categories_english_not_blank CHECK (length(trim(name_english)) >= 1),
    CONSTRAINT chk_expense_categories_notes_not_blank CHECK (notes IS NULL OR length(trim(notes)) >= 1),
    CONSTRAINT chk_expense_categories_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_expense_categories_updated_by CHECK (length(trim(updated_by)) >= 1)
);

INSERT INTO expense_categories (name_arabic, name_english, active) VALUES
    ('المغسلة', 'Laundry', TRUE),
    ('خدمات المبنى', 'Building services', TRUE),
    ('القهوة والضيافة', 'Coffee and hospitality', TRUE),
    ('الكهرباء', 'Electricity', TRUE),
    ('المياه', 'Water', TRUE),
    ('الصيانة', 'Maintenance', TRUE),
    ('المشتريات', 'Purchases', TRUE),
    ('المواصلات', 'Transportation', TRUE),
    ('أخرى', 'Other', TRUE);

CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    category_name_arabic VARCHAR(150) NOT NULL,
    category_name_english VARCHAR(150) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    business_date DATE NOT NULL,
    payment_method_id BIGINT NOT NULL,
    payment_method_code VARCHAR(50) NOT NULL,
    payment_method_name_arabic VARCHAR(150) NOT NULL,
    payment_method_name_english VARCHAR(150) NOT NULL,
    payment_method_type VARCHAR(20) NOT NULL,
    payment_reference VARCHAR(200),
    description VARCHAR(1000) NOT NULL,
    receipt_path VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_by VARCHAR(100),
    cancellation_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_expenses_category
        FOREIGN KEY (category_id) REFERENCES expense_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_payment_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT chk_expenses_category_arabic_not_blank CHECK (length(trim(category_name_arabic)) >= 1),
    CONSTRAINT chk_expenses_category_english_not_blank CHECK (length(trim(category_name_english)) >= 1),
    CONSTRAINT chk_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_expenses_payment_code_not_blank CHECK (length(trim(payment_method_code)) >= 1),
    CONSTRAINT chk_expenses_payment_arabic_not_blank CHECK (length(trim(payment_method_name_arabic)) >= 1),
    CONSTRAINT chk_expenses_payment_english_not_blank CHECK (length(trim(payment_method_name_english)) >= 1),
    CONSTRAINT chk_expenses_payment_type CHECK (payment_method_type IN ('CASH', 'CARD', 'WALLET', 'OTHER')),
    CONSTRAINT chk_expenses_reference_not_blank CHECK (
        payment_reference IS NULL OR length(trim(payment_reference)) >= 1
    ),
    CONSTRAINT chk_expenses_description_not_blank CHECK (length(trim(description)) >= 1),
    CONSTRAINT chk_expenses_receipt_path_not_blank CHECK (
        receipt_path IS NULL OR length(trim(receipt_path)) >= 1
    ),
    CONSTRAINT chk_expenses_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT chk_expenses_cancellation_state CHECK (
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
    CONSTRAINT chk_expenses_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_expenses_updated_by CHECK (length(trim(updated_by)) >= 1)
);

ALTER TABLE employee_transactions ADD COLUMN payment_method_id BIGINT;
ALTER TABLE employee_transactions ADD COLUMN payment_method_type VARCHAR(20);

UPDATE employee_transactions
SET payment_method_id = (
    SELECT payment_methods.id
    FROM payment_methods
    WHERE payment_methods.code = upper(trim(employee_transactions.payment_method))
)
WHERE payment_method IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM payment_methods
      WHERE payment_methods.code = upper(trim(employee_transactions.payment_method))
  );

UPDATE employee_transactions
SET payment_method_type = (
    SELECT payment_methods.payment_type
    FROM payment_methods
    WHERE payment_methods.id = employee_transactions.payment_method_id
)
WHERE payment_method_id IS NOT NULL;

ALTER TABLE employee_transactions
    ADD CONSTRAINT fk_employee_transactions_payment_method
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT;

ALTER TABLE employee_transactions
    ADD CONSTRAINT chk_employee_transactions_payment_type
    CHECK (payment_method_type IS NULL OR payment_method_type IN ('CASH', 'CARD', 'WALLET', 'OTHER'));

CREATE INDEX idx_payment_methods_active_sort
    ON payment_methods (active, sort_order, code);
CREATE INDEX idx_expense_categories_active_english
    ON expense_categories (active, name_english);
CREATE INDEX idx_expenses_date_status
    ON expenses (business_date, status);
CREATE INDEX idx_expenses_category_date_status
    ON expenses (category_id, business_date, status);
CREATE INDEX idx_expenses_payment_date_status
    ON expenses (payment_method_id, business_date, status);
CREATE INDEX idx_employee_transactions_payment_method
    ON employee_transactions (payment_method_id);
