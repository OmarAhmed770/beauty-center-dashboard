package com.omar.beautycenter.payment.dto;

import com.omar.beautycenter.payment.PaymentMethodType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePaymentMethodRequest(
        @NotBlank(message = "Arabic display name is required.")
        @Size(max = 150, message = "Arabic display name must not exceed 150 characters.")
        String displayNameArabic,

        @NotBlank(message = "English display name is required.")
        @Size(max = 150, message = "English display name must not exceed 150 characters.")
        String displayNameEnglish,

        @NotNull(message = "Payment method type is required.")
        PaymentMethodType type,

        @NotNull(message = "Reference-number requirement is required.")
        Boolean requiresReferenceNumber,

        @NotNull(message = "Sort order is required.")
        @Min(value = 0, message = "Sort order must not be negative.")
        @Max(value = 10000, message = "Sort order must not exceed 10000.")
        Integer sortOrder) {
}
