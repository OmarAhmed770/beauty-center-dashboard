package com.omar.beautycenter.discount.dto;

import com.omar.beautycenter.discount.DiscountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDiscountRequest(
        @NotBlank(message = "Discount name is required.")
        @Size(max = 150, message = "Discount name must not exceed 150 characters.")
        String name,

        @NotNull(message = "Discount type is required.")
        DiscountType type,

        @NotNull(message = "Discount value is required.")
        @PositiveOrZero(message = "Discount value must be zero or greater.")
        @Digits(integer = 17, fraction = 2, message = "Discount value must have at most 2 decimal digits.")
        BigDecimal value,

        @NotNull(message = "Start date is required.")
        LocalDate startDate,

        @NotNull(message = "End date is required.")
        LocalDate endDate,

        @NotNull(message = "Active status is required.")
        Boolean active,

        @Positive(message = "Catalog item id must be positive.")
        Long catalogItemId,

        @Positive(message = "Category id must be positive.")
        Long categoryId) {
}
