// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class AcceptanceUserSeederTest {

    @Test
    void createsExactlyTheThreeConfiguredAcceptanceUsersOnAnIsolatedDatabase() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        DataSource dataSource = isolatedPostgresDataSource();
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "hash-" + invocation.getArgument(0));

        AcceptanceUserSeeder seeder = new AcceptanceUserSeeder(
                completeProperties(), userRepository, passwordEncoder, dataSource);

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(3)).save(users.capture());
        assertThat(users.getAllValues())
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("acceptance.owner", "acceptance.full.staff", "acceptance.receptionist");
        assertThat(users.getAllValues())
                .allSatisfy(user -> {
                    assertThat(user.isActive()).isTrue();
                    assertThat(user.getDisplayName()).startsWith("Acceptance Test - ");
                    assertThat(user.getPasswordHash()).startsWith("hash-");
                });
        assertThat(users.getAllValues().stream()
                        .filter(user -> user.getUsername().equals("acceptance.owner"))
                        .findFirst()
                        .orElseThrow()
                        .getRoles())
                .containsExactly(Role.OWNER);
    }

    @Test
    void failsBeforeConnectingWhenAnyRequiredAcceptanceCredentialIsMissing() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        DataSource dataSource = mock(DataSource.class);
        AcceptanceSeedProperties incompleteProperties = new AcceptanceSeedProperties(
                true,
                "acceptance.owner",
                "acceptance-owner-password",
                "acceptance.full.staff",
                "acceptance-full-staff-password",
                "acceptance.receptionist",
                "");
        AcceptanceUserSeeder seeder = new AcceptanceUserSeeder(
                incompleteProperties, userRepository, passwordEncoder, dataSource);

        assertThatThrownBy(() -> seeder.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACCEPTANCE_RECEPTIONIST_PASSWORD");
        verify(dataSource, never()).getConnection();
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesToSeedWhenTheConnectedDatabaseIsNotExplicitlyAnAcceptanceDatabase() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getCatalog()).thenReturn("beauty_center");

        AcceptanceUserSeeder seeder = new AcceptanceUserSeeder(
                completeProperties(), userRepository, passwordEncoder, dataSource);

        assertThatThrownBy(() -> seeder.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("acceptance PostgreSQL database");
        verify(userRepository, never()).save(any());
    }

    private static AcceptanceSeedProperties completeProperties() {
        return new AcceptanceSeedProperties(
                true,
                "acceptance.owner",
                "acceptance-owner-password",
                "acceptance.full.staff",
                "acceptance-full-staff-password",
                "acceptance.receptionist",
                "acceptance-receptionist-password");
    }

    private static DataSource isolatedPostgresDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getCatalog()).thenReturn("beauty_center_acceptance");
        return dataSource;
    }
}
