package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.HairConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HairConfigurationResponse(
        Long id,
        Long hairItemId,
        String hairItemCode,
        String hairItemNameArabic,
        String hairItemNameEnglish,
        String brand,
        String length,
        String color,
        String unitType,
        BigDecimal unitSellingPrice,
        BigDecimal unitCost,
        BigDecimal installationFee,
        BigDecimal minimumQuantity,
        boolean stockConfigured,
        BigDecimal availableStockGrams,
        boolean active,
        LocalDate effectiveDate,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static HairConfigurationResponse from(
            HairConfiguration configuration,
            boolean includeCost,
            HairStockResponse stock) {
        return new HairConfigurationResponse(
                configuration.getId(),
                configuration.getHairItem().getId(),
                configuration.getHairItem().getCode(),
                configuration.getHairItem().getNameArabic(),
                configuration.getHairItem().getNameEnglish(),
                configuration.getBrand(),
                configuration.getLength(),
                configuration.getColor(),
                configuration.getUnitType(),
                configuration.getUnitSellingPrice(),
                includeCost ? configuration.getUnitCost() : null,
                configuration.getInstallationFee(),
                configuration.getMinimumQuantity(),
                stock != null && stock.stockConfigured(),
                stock == null ? null : stock.availableGrams(),
                configuration.isActive(),
                configuration.getEffectiveDate(),
                configuration.getVersion(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt(),
                configuration.getCreatedBy(),
                configuration.getUpdatedBy());
    }

    public static HairConfigurationResponse from(HairConfiguration configuration, boolean includeCost) {
        return from(configuration, includeCost, null);
    }
}
