package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.catalog.CatalogItemType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CatalogItemResponse(
        Long id,
        String code,
        String nameArabic,
        String nameEnglish,
        CatalogItemType itemType,
        Long categoryId,
        String categoryNameArabic,
        String categoryNameEnglish,
        BigDecimal currentSellingPrice,
        BigDecimal currentCost,
        boolean active,
        String description,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static CatalogItemResponse from(CatalogItem item, boolean includeCost) {
        return new CatalogItemResponse(
                item.getId(),
                item.getCode(),
                item.getNameArabic(),
                item.getNameEnglish(),
                item.getItemType(),
                item.getCategory().getId(),
                item.getCategory().getNameArabic(),
                item.getCategory().getNameEnglish(),
                item.getCurrentSellingPrice(),
                includeCost ? item.getCurrentCost() : null,
                item.isActive(),
                item.getDescription(),
                item.getVersion(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getCreatedBy(),
                item.getUpdatedBy());
    }
}
