package com.omar.beautycenter.expense.dto;

import jakarta.validation.constraints.NotNull;

public record ExpenseCategoryActiveRequest(
        @NotNull(message = "Active status is required.") Boolean active) {
}
