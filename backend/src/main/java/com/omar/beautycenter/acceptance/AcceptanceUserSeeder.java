package com.omar.beautycenter.acceptance;

import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import com.omar.beautycenter.user.UserService;
import com.omar.beautycenter.user.SystemPermission;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds only explicitly configured non-production accounts for browser acceptance checks.
 *
 * <p>The profile and flag conditions are deliberately both required. This component also refuses
 * to write unless the connected PostgreSQL database name contains {@code acceptance}.
 */
@Component
@Profile("acceptance")
@Order(110)
@ConditionalOnProperty(prefix = "app.acceptance-seed", name = "enabled", havingValue = "true")
public class AcceptanceUserSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceUserSeeder.class);
    private static final String ACCEPTANCE_DATABASE_TOKEN = "acceptance";
    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final int BCRYPT_MAXIMUM_PASSWORD_BYTES = 72;

    private final AcceptanceSeedProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    public AcceptanceUserSeeder(
            AcceptanceSeedProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DataSource dataSource) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<AcceptanceUser> users = configuredUsers();
        verifyIsolatedAcceptanceDatabase();

        users.forEach(this::createOrUpdate);
        LOGGER.info("Acceptance users are ready for local browser testing.");
    }

    private List<AcceptanceUser> configuredUsers() {
        return List.of(
                acceptanceUser(
                        "ACCEPTANCE_OWNER_USERNAME",
                        properties.ownerUsername(),
                        "ACCEPTANCE_OWNER_PASSWORD",
                        properties.ownerPassword(),
                        Role.OWNER,
                        false),
                acceptanceUser(
                        "ACCEPTANCE_FULL_STAFF_USERNAME",
                        properties.fullStaffUsername(),
                        "ACCEPTANCE_FULL_STAFF_PASSWORD",
                        properties.fullStaffPassword(),
                        Role.RECEPTIONIST,
                        true),
                acceptanceUser(
                        "ACCEPTANCE_RECEPTIONIST_USERNAME",
                        properties.receptionistUsername(),
                        "ACCEPTANCE_RECEPTIONIST_PASSWORD",
                        properties.receptionistPassword(),
                        Role.RECEPTIONIST,
                        false));
    }

    private AcceptanceUser acceptanceUser(
            String usernameVariable,
            String username,
            String passwordVariable,
            String rawPassword,
            Role role,
            boolean fullAccessStaff) {
        String normalizedUsername = requiredUsername(usernameVariable, username);
        validatePasswordVariable(passwordVariable, rawPassword);
        return new AcceptanceUser(normalizedUsername, rawPassword, role, fullAccessStaff);
    }

    private void createOrUpdate(AcceptanceUser acceptanceUser) {
        User user = userRepository.findByUsername(acceptanceUser.username()).orElse(null);
        String passwordHash = passwordEncoder.encode(acceptanceUser.rawPassword());
        if (user == null) {
            user = new User(
                    acceptanceUser.username(),
                    displayNameFor(acceptanceUser.role()),
                    passwordHash,
                    true,
                    Set.of(acceptanceUser.role()));
        }

        user.changeDisplayName(displayNameFor(acceptanceUser.role()));
        user.changePasswordHash(passwordHash);
        user.activate();
        user.setRoles(Set.of(acceptanceUser.role()));
        user.setPermissions(acceptanceUser.fullAccessStaff()
                ? Set.copyOf(java.util.EnumSet.allOf(SystemPermission.class))
                : Set.of(SystemPermission.ADD_ORDERS));
        userRepository.save(user);
    }

    private void verifyIsolatedAcceptanceDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String productName = metadata.getDatabaseProductName();
            String databaseName = connection.getCatalog();
            if (!"PostgreSQL".equalsIgnoreCase(productName)
                    || databaseName == null
                    || !databaseName.toLowerCase(Locale.ROOT).contains(ACCEPTANCE_DATABASE_TOKEN)) {
                throw new IllegalStateException(
                        "Acceptance user seeding stopped because the connected database is not an explicitly named acceptance PostgreSQL database.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Acceptance user seeding stopped because the isolated PostgreSQL database could not be verified.",
                    exception);
        }
    }

    private static String requiredUsername(String variableName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required acceptance variable is missing: " + variableName);
        }
        return UserService.normalizeUsername(value);
    }

    private static void validatePasswordVariable(String variableName, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "Required acceptance password variable must contain at least 12 characters: "
                            + variableName);
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_PASSWORD_BYTES) {
            throw new IllegalStateException(
                    "Required acceptance password variable must not exceed 72 UTF-8 bytes: "
                            + variableName);
        }
    }

    private static String displayNameFor(Role role) {
        return "Acceptance Test - " + role.name();
    }

    private record AcceptanceUser(
            String username,
            String rawPassword,
            Role role,
            boolean fullAccessStaff) {
    }
}
