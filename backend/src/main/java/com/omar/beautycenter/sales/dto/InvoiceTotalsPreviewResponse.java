package com.omar.beautycenter.sales.dto;

import java.math.BigDecimal;

public record InvoiceTotalsPreviewResponse(
        Long invoiceId,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal finalTotal,
        BigDecimal paymentTotal,
        BigDecimal remainingPayment) {
}
