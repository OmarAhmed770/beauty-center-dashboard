package com.omar.beautycenter.payment.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentMethodActiveRequest(
        @NotNull(message = "Active status is required.") Boolean active) {
}
