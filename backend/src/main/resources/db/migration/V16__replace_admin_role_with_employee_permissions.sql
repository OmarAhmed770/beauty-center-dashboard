CREATE TABLE user_permissions (
    user_id BIGINT NOT NULL,
    permission VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission),
    CONSTRAINT fk_user_permissions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_permissions_permission CHECK (permission IN (
        'DASHBOARD', 'ADD_ORDERS', 'EXPENSES', 'SERVICES_AND_PRICES',
        'EMPLOYEES', 'DAILY_CLOSING', 'REPORTS', 'INVOICES_AND_RETURNS',
        'SETTINGS', 'BACKUP_MANAGEMENT'
    ))
);

CREATE INDEX idx_user_permissions_permission ON user_permissions (permission);

-- Preserve every existing ADMIN's operational access before converting it to a staff account.
INSERT INTO user_permissions (user_id, permission)
SELECT admin_roles.user_id, permissions.permission
FROM user_roles admin_roles
CROSS JOIN (
    SELECT 'DASHBOARD' AS permission UNION ALL
    SELECT 'ADD_ORDERS' UNION ALL
    SELECT 'EXPENSES' UNION ALL
    SELECT 'SERVICES_AND_PRICES' UNION ALL
    SELECT 'EMPLOYEES' UNION ALL
    SELECT 'DAILY_CLOSING' UNION ALL
    SELECT 'REPORTS' UNION ALL
    SELECT 'INVOICES_AND_RETURNS' UNION ALL
    SELECT 'SETTINGS' UNION ALL
    SELECT 'BACKUP_MANAGEMENT'
) permissions
WHERE admin_roles.role = 'ADMIN';

-- Existing reception users keep their daily order capability after the new model is enabled.
INSERT INTO user_permissions (user_id, permission)
SELECT reception_roles.user_id, 'ADD_ORDERS'
FROM user_roles reception_roles
WHERE reception_roles.role = 'RECEPTIONIST'
  AND NOT EXISTS (
      SELECT 1
      FROM user_permissions current_permission
      WHERE current_permission.user_id = reception_roles.user_id
        AND current_permission.permission = 'ADD_ORDERS'
  );

-- A user could theoretically have both legacy roles. Keep the staff row without violating the key.
DELETE FROM user_roles admin_roles
WHERE admin_roles.role = 'ADMIN'
  AND EXISTS (
      SELECT 1
      FROM user_roles staff_roles
      WHERE staff_roles.user_id = admin_roles.user_id
        AND staff_roles.role = 'RECEPTIONIST'
  );

UPDATE user_roles
SET role = 'RECEPTIONIST'
WHERE role = 'ADMIN';

ALTER TABLE user_roles DROP CONSTRAINT chk_user_roles_role;

ALTER TABLE user_roles
    ADD CONSTRAINT chk_user_roles_role
    CHECK (role IN ('OWNER', 'RECEPTIONIST'));

-- This is intentionally a data-safety guard: an inconsistent installation stops its
-- migration instead of silently demoting an owner.
CREATE UNIQUE INDEX uk_user_roles_single_owner
    ON user_roles (role)
    WHERE role = 'OWNER';
