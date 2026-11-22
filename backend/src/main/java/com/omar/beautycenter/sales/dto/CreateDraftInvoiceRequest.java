package com.omar.beautycenter.sales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateDraftInvoiceRequest(
        @NotNull(message = "Business date is required.")
        LocalDate businessDate,

        @Size(max = 150, message = "Customer name must not exceed 150 characters.")
        String customerName,

        @Size(max = 50, message = "Customer phone must not exceed 50 characters.")
        String customerPhone,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes) {
}
