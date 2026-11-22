package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.PriceHistory;
import com.omar.beautycenter.catalog.PriceValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryResponse(
        Long id,
        Long catalogItemId,
        String catalogItemCode,
        String catalogItemNameArabic,
        String catalogItemNameEnglish,
        PriceValueType valueType,
        BigDecimal oldValue,
        BigDecimal newValue,
        LocalDateTime effectiveAt,
        String changeReason,
        String changedBy,
        LocalDateTime createdAt) {

    public static PriceHistoryResponse from(PriceHistory history) {
        return new PriceHistoryResponse(
                history.getId(),
                history.getCatalogItem().getId(),
                history.getCatalogItem().getCode(),
                history.getCatalogItem().getNameArabic(),
                history.getCatalogItem().getNameEnglish(),
                history.getValueType(),
                history.getOldValue(),
                history.getNewValue(),
                history.getEffectiveAt(),
                history.getChangeReason(),
                history.getChangedBy(),
                history.getCreatedAt());
    }
}
