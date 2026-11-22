package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.HairPriceOverrideAudit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HairPriceOverrideAuditResponse(
        Long id,
        Long configurationId,
        Long hairItemId,
        String hairItemCode,
        BigDecimal quantity,
        BigDecimal originalAmount,
        BigDecimal overriddenAmount,
        String overrideReason,
        String authorizedBy,
        LocalDateTime occurredAt) {

    public static HairPriceOverrideAuditResponse from(HairPriceOverrideAudit audit) {
        return new HairPriceOverrideAuditResponse(
                audit.getId(),
                audit.getHairConfiguration().getId(),
                audit.getHairConfiguration().getHairItem().getId(),
                audit.getHairConfiguration().getHairItem().getCode(),
                audit.getQuantity(),
                audit.getOriginalAmount(),
                audit.getOverriddenAmount(),
                audit.getOverrideReason(),
                audit.getAuthorizedBy(),
                audit.getOccurredAt());
    }
}
