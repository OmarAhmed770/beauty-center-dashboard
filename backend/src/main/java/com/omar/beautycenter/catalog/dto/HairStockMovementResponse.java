package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.HairStockMovement;
import com.omar.beautycenter.catalog.HairStockMovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HairStockMovementResponse(
        Long id,
        HairStockMovementType movementType,
        BigDecimal previousGrams,
        BigDecimal adjustmentGrams,
        BigDecimal newGrams,
        String reason,
        String createdBy,
        LocalDateTime createdAt) {

    public static HairStockMovementResponse from(HairStockMovement movement) {
        return new HairStockMovementResponse(
                movement.getId(), movement.getMovementType(), movement.getPreviousGrams(),
                movement.getAdjustmentGrams(), movement.getNewGrams(), movement.getReason(),
                movement.getCreatedBy(), movement.getCreatedAt());
    }
}
