package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.discount.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HairCalculationPreviewResponse(
        Long configurationId,
        Long hairItemId,
        String hairItemCode,
        BigDecimal quantity,
        LocalDate calculationDate,
        BigDecimal baseProductAmount,
        BigDecimal installationFee,
        BigDecimal subtotal,
        Long discountId,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal calculatedFinalAmount,
        boolean overridden,
        BigDecimal finalAmount,
        BigDecimal costEstimate) {
}
