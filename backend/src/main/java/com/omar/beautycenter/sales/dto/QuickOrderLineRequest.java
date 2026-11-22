package com.omar.beautycenter.sales.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** A catalog-backed line for the cashier quick-order endpoint. */
@JsonIgnoreProperties({"price", "total", "unitPrice", "unitSellingPrice", "unitCost"})
public record QuickOrderLineRequest(
        @NotNull(message = "Catalog item id is required.")
        @Positive(message = "Catalog item id must be positive.")
        Long catalogItemId,

        @NotNull(message = "Quantity is required.")
        @Positive(message = "Quantity must be greater than zero.")
        @Digits(integer = 16, fraction = 3, message = "Quantity must have at most 3 decimal digits.")
        BigDecimal quantity,

        @Positive(message = "Employee id must be positive.")
        Long employeeId,

        @Positive(message = "Hair configuration id must be positive.")
        Long hairConfigurationId,

        @Positive(message = "Hair grams must be greater than zero.")
        @Digits(integer = 16, fraction = 3, message = "Hair grams must have at most 3 decimal digits.")
        BigDecimal hairGrams) {
}
