ALTER TABLE user_roles DROP CONSTRAINT chk_user_roles_role;

UPDATE user_roles
SET role = 'ADMIN'
WHERE role = 'MANAGER';

ALTER TABLE user_roles
    ADD CONSTRAINT chk_user_roles_role
    CHECK (role IN ('OWNER', 'ADMIN', 'RECEPTIONIST'));

ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN locked_until TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE users
    ADD CONSTRAINT chk_users_failed_login_attempts_nonnegative
        CHECK (failed_login_attempts >= 0);

CREATE TABLE app_setup_state (
    id BIGINT PRIMARY KEY,
    initial_owner_setup_completed BOOLEAN NOT NULL,
    CONSTRAINT chk_app_setup_state_singleton CHECK (id = 1)
);

INSERT INTO app_setup_state (id, initial_owner_setup_completed)
SELECT 1, CASE WHEN EXISTS (SELECT 1 FROM users) THEN TRUE ELSE FALSE END;
