package com.omar.beautycenter.closing;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Serializes financial writes and daily closing for one business date in PostgreSQL.
 * H2 is used only by isolated tests and does not support PostgreSQL advisory locks.
 */
@Component
public class BusinessDateTransactionLock {

    private static final int LOCK_NAMESPACE = 0x42434D53; // "BCMS"

    private final JdbcTemplate jdbcTemplate;
    private final boolean postgresql;

    public BusinessDateTransactionLock(
            JdbcTemplate jdbcTemplate,
            DataSourceProperties dataSourceProperties) {
        this.jdbcTemplate = jdbcTemplate;
        String jdbcUrl = dataSourceProperties.determineUrl();
        this.postgresql = jdbcUrl != null
                && jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql:");
    }

    public void acquire(LocalDate businessDate) {
        Objects.requireNonNull(businessDate, "businessDate is required");
        if (!postgresql) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Business-date locks must be acquired inside a database transaction.");
        }
        int dateKey = Math.toIntExact(businessDate.toEpochDay());
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(?, ?)")) {
                statement.setInt(1, LOCK_NAMESPACE);
                statement.setInt(2, dateKey);
                statement.execute();
            }
            return null;
        });
    }
}
