package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.sales.Invoice;
import com.omar.beautycenter.sales.InvoiceItem;
import java.math.BigDecimal;
import java.util.List;

/** Minimal cashier response without cost or profit fields. */
public record QuickOrderResponse(
        Long invoiceId,
        String invoiceNumber,
        BigDecimal total,
        List<QuickOrderLineResponse> lines) {

    public static QuickOrderResponse from(Invoice invoice, List<InvoiceItem> items) {
        return new QuickOrderResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getFinalTotal(),
                items.stream().map(QuickOrderLineResponse::from).toList());
    }
}
