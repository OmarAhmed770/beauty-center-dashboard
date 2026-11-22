package com.omar.beautycenter.closing.dto;

import com.omar.beautycenter.closing.CashAdjustment;
import com.omar.beautycenter.closing.CashAdjustmentStatus;
import com.omar.beautycenter.closing.CashAdjustmentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CashAdjustmentResponse(
        Long id,
        LocalDate businessDate,
        CashAdjustmentType type,
        BigDecimal amount,
        String reason,
        CashAdjustmentStatus status,
        LocalDateTime cancelledAt,
        String cancelledBy,
        String cancellationReason,
        LocalDateTime createdAt,
        String createdBy) {

    public static CashAdjustmentResponse from(CashAdjustment adjustment) {
        return new CashAdjustmentResponse(
                adjustment.getId(), adjustment.getBusinessDate(), adjustment.getType(), adjustment.getAmount(),
                adjustment.getReason(), adjustment.getStatus(), adjustment.getCancelledAt(),
                adjustment.getCancelledBy(), adjustment.getCancellationReason(), adjustment.getCreatedAt(),
                adjustment.getCreatedBy());
    }
}
