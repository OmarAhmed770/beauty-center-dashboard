package com.omar.beautycenter.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Catalog-backed one-action cashier order. Prices and totals are deliberately absent: the server
 * reads them from the current catalog and persists the normal immutable invoice snapshots.
 */
public record CreateQuickOrderRequest(
        @NotNull(message = "Business date is required.")
        LocalDate businessDate,

        @NotEmpty(message = "At least one order line is required.")
        @Size(max = 50, message = "An order cannot contain more than 50 lines.")
        List<@Valid QuickOrderLineRequest> lines,

        @Size(max = 20, message = "An order cannot contain more than 20 payments.")
        List<@Valid QuickOrderPaymentRequest> payments,

        @NotBlank(message = "Client request id is required.")
        @Size(max = 100, message = "Client request id must not exceed 100 characters.")
        String clientRequestId,

        @Size(max = 150, message = "Customer name must not exceed 150 characters.")
        String customerName,

        @Size(max = 50, message = "Customer phone must not exceed 50 characters.")
        @Pattern(regexp = "^[+0-9][0-9 ()-]{5,49}$", message = "Customer phone format is invalid.")
        String customerPhone,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes) {
}
