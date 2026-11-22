// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.omar.beautycenter.H2FlywayCompatibilityConfiguration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;
import org.flywaydb.core.internal.resource.StringResource;
import org.junit.jupiter.api.Test;

class UserPermissionMigrationTest {

    @Test
    void productionMigrationChecksumsRemainCompatibleWithAppliedPostgresqlHistory() throws Exception {
        assertThat(checksum("V16__replace_admin_role_with_employee_permissions.sql"))
                .isEqualTo(H2FlywayCompatibilityConfiguration.PRODUCTION_V16_CHECKSUM);
        assertThat(checksum("V17__create_offline_backup_configuration_and_history.sql"))
                .isEqualTo(-1916062438);
    }

    @Test
    void v16ConvertsExistingAdminsToStaffWithExplicitPermissionsAndPreservesUsers() throws Exception {
        String databaseUrl = "jdbc:h2:mem:permission_migration_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .locations("classpath:db/migration")
                .target("15")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
            long ownerId = insertUser(connection, "owner.existing");
            long adminId = insertUser(connection, "admin.existing");
            long receptionistId = insertUser(connection, "reception.existing");
            insertRole(connection, ownerId, "OWNER");
            insertRole(connection, adminId, "ADMIN");
            insertRole(connection, receptionistId, "RECEPTIONIST");

            H2FlywayCompatibilityConfiguration.migrateThroughV16(
                    new org.springframework.jdbc.datasource.DriverManagerDataSource(databaseUrl, "sa", ""));

            assertThat(roleOf(connection, ownerId)).isEqualTo("OWNER");
            assertThat(roleOf(connection, adminId)).isEqualTo("RECEPTIONIST");
            assertThat(roleOf(connection, receptionistId)).isEqualTo("RECEPTIONIST");
            assertThat(countPermissions(connection, adminId)).isEqualTo(SystemPermission.values().length);
            assertThat(hasPermission(connection, receptionistId, SystemPermission.ADD_ORDERS.name())).isTrue();
            assertThat(countUsers(connection)).isEqualTo(3);
        }
    }

    private static long insertUser(Connection connection, String username) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, display_name, password_hash) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, username);
            statement.setString(3, "test-password-hash");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void insertRole(Connection connection, long userId, String role) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO user_roles (user_id, role) VALUES (?, ?)")) {
            statement.setLong(1, userId);
            statement.setString(2, role);
            statement.executeUpdate();
        }
    }

    private static String roleOf(Connection connection, long userId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT role FROM user_roles WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private static int countPermissions(Connection connection, long userId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM user_permissions WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static boolean hasPermission(Connection connection, long userId, String permission) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM user_permissions WHERE user_id = ? AND permission = ?")) {
            statement.setLong(1, userId);
            statement.setString(2, permission);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) == 1;
            }
        }
    }

    private static int countUsers(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private static int checksum(String migrationFile) throws Exception {
        try (var stream = UserPermissionMigrationTest.class.getClassLoader()
                .getResourceAsStream("db/migration/" + migrationFile)) {
            assertThat(stream).as("Migration resource %s", migrationFile).isNotNull();
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return ChecksumCalculator.calculate(new StringResource(content));
        }
    }
}
