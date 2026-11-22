package com.omar.beautycenter.closing.dto;

import com.omar.beautycenter.closing.DailyClosingStatus;
import com.omar.beautycenter.report.dto.FinancialSummaryResponse;
import java.time.LocalDate;

public record DailyClosingPreviewResponse(
        LocalDate businessDate,
        DailyClosingStatus status,
        FinancialSummaryResponse liveFinancials) {
}
