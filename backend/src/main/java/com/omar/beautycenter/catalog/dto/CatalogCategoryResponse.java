package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.CatalogCategory;
import java.time.LocalDateTime;

public record CatalogCategoryResponse(
        Long id,
        String nameArabic,
        String nameEnglish,
        boolean active,
        String description,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static CatalogCategoryResponse from(CatalogCategory category) {
        return new CatalogCategoryResponse(
                category.getId(),
                category.getNameArabic(),
                category.getNameEnglish(),
                category.isActive(),
                category.getDescription(),
                category.getVersion(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getCreatedBy(),
                category.getUpdatedBy());
    }
}
