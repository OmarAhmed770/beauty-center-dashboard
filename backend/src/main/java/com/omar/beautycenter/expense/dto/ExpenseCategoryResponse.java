package com.omar.beautycenter.expense.dto;

import com.omar.beautycenter.expense.ExpenseCategory;
import java.time.LocalDateTime;

public record ExpenseCategoryResponse(
        Long id,
        String nameArabic,
        String nameEnglish,
        boolean active,
        String notes,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static ExpenseCategoryResponse from(ExpenseCategory category) {
        return new ExpenseCategoryResponse(
                category.getId(),
                category.getNameArabic(),
                category.getNameEnglish(),
                category.isActive(),
                category.getNotes(),
                category.getVersion(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getCreatedBy(),
                category.getUpdatedBy());
    }
}
