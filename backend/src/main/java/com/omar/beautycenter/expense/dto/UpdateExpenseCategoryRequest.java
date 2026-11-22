package com.omar.beautycenter.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateExpenseCategoryRequest(
        @NotBlank(message = "Arabic name is required.")
        @Size(max = 150, message = "Arabic name must not exceed 150 characters.")
        String nameArabic,

        @NotBlank(message = "English name is required.")
        @Size(max = 150, message = "English name must not exceed 150 characters.")
        String nameEnglish,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes) {
}
