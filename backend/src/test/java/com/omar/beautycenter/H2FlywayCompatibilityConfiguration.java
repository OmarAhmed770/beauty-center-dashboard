package com.omar.beautycenter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * H2 has no partial-index support. Production PostgreSQL must keep V16 verbatim
 * because its checksum is already recorded. This test-only strategy applies an
 * equivalent one-OWNER constraint before allowing Flyway to continue with V17.
 */
@Configuration(proxyBeanMethods = false)
@Profile("test")
public class H2FlywayCompatibilityConfiguration {

    public static final int PRODUCTION_V16_CHECKSUM = 760630169;
    private static final String V16_SCRIPT = "V16__replace_admin_role_with_employee_permissions.sql";

    @Bean
    FlywayMigrationStrategy h2FlywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            try {
                if (!isH2(dataSource)) {
                    flyway.migrate();
                    return;
                }
                migrateThroughV16(dataSource);
                flyway.migrate();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not prepare the H2 Flyway compatibility schema.", exception);
            }
        };
    }

    public static void migrateThroughV16(DataSource dataSource) throws Exception {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("15")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection()) {
            if (isVersionApplied(connection, "16")) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
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
                        )
                        """);
                statement.execute("CREATE INDEX idx_user_permissions_permission ON user_permissions (permission)");
                statement.execute("""
                        INSERT INTO user_permissions (user_id, permission)
                        SELECT admin_roles.user_id, permissions.permission
                        FROM user_roles admin_roles
                        CROSS JOIN (
                            SELECT 'DASHBOARD' AS permission UNION ALL SELECT 'ADD_ORDERS'
                            UNION ALL SELECT 'EXPENSES' UNION ALL SELECT 'SERVICES_AND_PRICES'
                            UNION ALL SELECT 'EMPLOYEES' UNION ALL SELECT 'DAILY_CLOSING'
                            UNION ALL SELECT 'REPORTS' UNION ALL SELECT 'INVOICES_AND_RETURNS'
                            UNION ALL SELECT 'SETTINGS' UNION ALL SELECT 'BACKUP_MANAGEMENT'
                        ) permissions
                        WHERE admin_roles.role = 'ADMIN'
                        """);
                statement.execute("""
                        INSERT INTO user_permissions (user_id, permission)
                        SELECT reception_roles.user_id, 'ADD_ORDERS'
                        FROM user_roles reception_roles
                        WHERE reception_roles.role = 'RECEPTIONIST'
                          AND NOT EXISTS (
                              SELECT 1 FROM user_permissions current_permission
                              WHERE current_permission.user_id = reception_roles.user_id
                                AND current_permission.permission = 'ADD_ORDERS'
                          )
                        """);
                statement.execute("""
                        DELETE FROM user_roles admin_roles
                        WHERE admin_roles.role = 'ADMIN'
                          AND EXISTS (
                              SELECT 1 FROM user_roles staff_roles
                              WHERE staff_roles.user_id = admin_roles.user_id
                                AND staff_roles.role = 'RECEPTIONIST'
                          )
                        """);
                statement.execute("UPDATE user_roles SET role = 'RECEPTIONIST' WHERE role = 'ADMIN'");
                statement.execute("ALTER TABLE user_roles DROP CONSTRAINT chk_user_roles_role");
                statement.execute("""
                        ALTER TABLE user_roles ADD CONSTRAINT chk_user_roles_role
                        CHECK (role IN ('OWNER', 'RECEPTIONIST'))
                        """);
                statement.execute("""
                        ALTER TABLE user_roles ADD COLUMN owner_role_guard VARCHAR(30)
                        GENERATED ALWAYS AS (CASE WHEN role = 'OWNER' THEN role ELSE NULL END)
                        """);
                statement.execute("CREATE UNIQUE INDEX uk_user_roles_single_owner ON user_roles (owner_role_guard)");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO flyway_schema_history (
                        installed_rank, version, description, type, script, checksum,
                        installed_by, installed_on, execution_time, success
                    ) VALUES (
                        (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
                        '16', 'replace admin role with employee permissions', 'SQL', ?, ?,
                        CURRENT_USER, CURRENT_TIMESTAMP, 0, TRUE
                    )
                    """)) {
                statement.setString(1, V16_SCRIPT);
                statement.setInt(2, PRODUCTION_V16_CHECKSUM);
                statement.executeUpdate();
            }
        }
    }

    private static boolean isH2(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            return "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }

    private static boolean isVersionApplied(Connection connection, String version) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ?")) {
            statement.setString(1, version);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }
}
