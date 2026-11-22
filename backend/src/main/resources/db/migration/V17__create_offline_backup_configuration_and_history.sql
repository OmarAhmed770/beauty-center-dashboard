CREATE TABLE backup_settings (
    id BIGINT PRIMARY KEY,
    backup_directory VARCHAR(1000) NOT NULL,
    automatic_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    automatic_time TIME WITHOUT TIME ZONE NOT NULL DEFAULT '02:00:00',
    retention_count INTEGER NOT NULL DEFAULT 30,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_backup_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_backup_settings_directory_not_blank CHECK (length(trim(backup_directory)) >= 1),
    CONSTRAINT chk_backup_settings_retention CHECK (retention_count BETWEEN 1 AND 365),
    CONSTRAINT chk_backup_settings_updated_by_not_blank CHECK (length(trim(updated_by)) >= 1)
);

CREATE TABLE database_backups (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    backup_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    file_size_bytes BIGINT,
    sha256 VARCHAR(64),
    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    error_message VARCHAR(1000),
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_database_backups_file_name UNIQUE (file_name),
    CONSTRAINT chk_database_backups_type CHECK (backup_type IN ('MANUAL', 'AUTOMATIC', 'SAFETY')),
    CONSTRAINT chk_database_backups_status CHECK (status IN ('SUCCESS', 'FAILED', 'RUNNING')),
    CONSTRAINT chk_database_backups_size CHECK (file_size_bytes IS NULL OR file_size_bytes > 0),
    CONSTRAINT chk_database_backups_checksum CHECK (sha256 IS NULL OR length(sha256) = 64),
    CONSTRAINT chk_database_backups_created_by_not_blank CHECK (length(trim(created_by)) >= 1)
);

CREATE INDEX idx_database_backups_completed_at ON database_backups (completed_at DESC);
CREATE INDEX idx_database_backups_type_started_at ON database_backups (backup_type, started_at DESC);
