package com.omar.beautycenter.discount.dto;

import com.omar.beautycenter.discount.Discount;
import com.omar.beautycenter.discount.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiscountResponse(
        Long id,
        String name,
        DiscountType type,
        BigDecimal value,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        Long catalogItemId,
        String catalogItemCode,
        Long categoryId,
        String categoryNameArabic,
        String categoryNameEnglish,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static DiscountResponse from(Discount discount) {
        return new DiscountResponse(
                discount.getId(),
                discount.getName(),
                discount.getType(),
                discount.getValue(),
                discount.getStartDate(),
                discount.getEndDate(),
                discount.isActive(),
                discount.getCatalogItem() == null ? null : discount.getCatalogItem().getId(),
                discount.getCatalogItem() == null ? null : discount.getCatalogItem().getCode(),
                discount.getCategory() == null ? null : discount.getCategory().getId(),
                discount.getCategory() == null ? null : discount.getCategory().getNameArabic(),
                discount.getCategory() == null ? null : discount.getCategory().getNameEnglish(),
                discount.getVersion(),
                discount.getCreatedAt(),
                discount.getUpdatedAt(),
                discount.getCreatedBy(),
                discount.getUpdatedBy());
    }
}
