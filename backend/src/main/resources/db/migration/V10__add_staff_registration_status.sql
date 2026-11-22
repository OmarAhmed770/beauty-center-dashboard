ALTER TABLE users
    ADD COLUMN phone VARCHAR(50);

ALTER TABLE users
    ADD COLUMN registration_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE users
    ADD CONSTRAINT chk_users_registration_status
        CHECK (registration_status IN ('PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE users
    ADD CONSTRAINT chk_users_phone_not_blank
        CHECK (phone IS NULL OR length(trim(phone)) >= 1);

CREATE INDEX idx_users_registration_status ON users (registration_status);
