package com.omar.beautycenter.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SetInvoicePaymentsRequest(
        @NotNull(message = "Payments are required.")
        @Size(max = 20, message = "An invoice cannot contain more than 20 payments.")
        List<@Valid InvoicePaymentRequest> payments) {
}
