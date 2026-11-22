package com.omar.beautycenter.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.omar.beautycenter.common.error.BusinessRuleException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateRangeResolverTest {

    private final DateRangeResolver resolver = new DateRangeResolver(Clock.fixed(
            Instant.parse("2026-07-22T10:00:00Z"), ZoneId.of("Africa/Cairo")));

    @Test
    void resolvesAllPresetFiltersUsingMondayAsWeekStart() {
        assertThat(resolver.resolve(DashboardFilter.TODAY, null, null))
                .isEqualTo(new DateRange(LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 22)));
        assertThat(resolver.resolve(DashboardFilter.YESTERDAY, null, null))
                .isEqualTo(new DateRange(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 21)));
        assertThat(resolver.resolve(DashboardFilter.CURRENT_WEEK, null, null))
                .isEqualTo(new DateRange(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22)));
        assertThat(resolver.resolve(DashboardFilter.CURRENT_MONTH, null, null))
                .isEqualTo(new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 22)));
    }

    @Test
    void customRangeRequiresOrderedEndpoints() {
        assertThatThrownBy(() -> resolver.resolve(
                DashboardFilter.CUSTOM, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("fromDate must be on or before toDate.");
    }
}
