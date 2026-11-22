CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_username_not_blank CHECK (length(trim(username)) >= 3),
    CONSTRAINT chk_users_username_normalized CHECK (username = lower(trim(username))),
    CONSTRAINT chk_users_display_name_not_blank CHECK (length(trim(display_name)) >= 1),
    CONSTRAINT chk_users_created_by_not_blank CHECK (length(trim(created_by)) >= 1),
    CONSTRAINT chk_users_updated_by_not_blank CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_roles_role
        CHECK (role IN ('OWNER', 'MANAGER', 'RECEPTIONIST'))
);

CREATE INDEX idx_users_active ON users (active);
CREATE INDEX idx_user_roles_role ON user_roles (role);
