package com.omar.beautycenter.discount.dto;

import jakarta.validation.constraints.NotNull;

public record DiscountActiveRequest(
        @NotNull(message = "Active status is required.")
        Boolean active) {
}
