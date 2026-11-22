CREATE TABLE business_profile (
    id BIGINT PRIMARY KEY,
    business_name VARCHAR(150) NOT NULL,
    logo_data_url TEXT,
    phone VARCHAR(50),
    address VARCHAR(500),
    currency VARCHAR(3) NOT NULL,
    receipt_information VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_business_profile_singleton CHECK (id = 1),
    CONSTRAINT chk_business_profile_name CHECK (length(trim(business_name)) >= 1),
    CONSTRAINT chk_business_profile_currency CHECK (currency IN ('EGP', 'SAR', 'AED', 'USD', 'EUR')),
    CONSTRAINT chk_business_profile_logo CHECK (
        logo_data_url IS NULL OR length(trim(logo_data_url)) >= 1
    ),
    CONSTRAINT chk_business_profile_phone CHECK (
        phone IS NULL OR length(trim(phone)) >= 1
    ),
    CONSTRAINT chk_business_profile_address CHECK (
        address IS NULL OR length(trim(address)) >= 1
    ),
    CONSTRAINT chk_business_profile_receipt_information CHECK (
        receipt_information IS NULL OR length(trim(receipt_information)) >= 1
    ),
    CONSTRAINT chk_business_profile_created_by CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_business_profile_updated_by CHECK (length(trim(updated_by)) >= 1)
);

INSERT INTO business_profile (
    id, business_name, currency, receipt_information
) VALUES (
    1, 'مركز التجميل', 'EGP', 'شكرًا لزيارتكم'
);
