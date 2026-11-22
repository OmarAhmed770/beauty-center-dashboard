package com.omar.beautycenter.report;

import com.omar.beautycenter.common.error.BusinessRuleException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Service;

@Service
public class DateRangeResolver {

    private final Clock clock;

    public DateRangeResolver(Clock clock) {
        this.clock = clock;
    }

    public DateRange resolve(
            DashboardFilter filter,
            LocalDate customFrom,
            LocalDate customTo) {
        DashboardFilter selected = filter == null ? DashboardFilter.TODAY : filter;
        LocalDate today = LocalDate.now(clock);
        return switch (selected) {
            case TODAY -> new DateRange(today, today);
            case YESTERDAY -> new DateRange(today.minusDays(1), today.minusDays(1));
            case CURRENT_WEEK -> new DateRange(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
            case CURRENT_MONTH -> new DateRange(today.withDayOfMonth(1), today);
            case CUSTOM -> required(customFrom, customTo);
        };
    }

    public DateRange required(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BusinessRuleException("Both fromDate and toDate are required.");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("fromDate must be on or before toDate.");
        }
        return new DateRange(fromDate, toDate);
    }
}
