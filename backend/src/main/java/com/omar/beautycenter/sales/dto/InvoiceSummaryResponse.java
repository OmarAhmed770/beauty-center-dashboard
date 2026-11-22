package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.sales.Invoice;
import com.omar.beautycenter.sales.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceSummaryResponse(
        Long id,
        String invoiceNumber,
        LocalDate businessDate,
        String customerName,
        String customerPhone,
        InvoiceStatus status,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal finalTotal,
        BigDecimal refundedTotal,
        String createdBy,
        LocalDateTime createdAt,
        String completedBy,
        LocalDateTime completedAt) {

    public static InvoiceSummaryResponse from(Invoice invoice) {
        return new InvoiceSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getBusinessDate(),
                invoice.getCustomerName(),
                invoice.getCustomerPhone(),
                invoice.getStatus(),
                invoice.getSubtotal(),
                invoice.getDiscountTotal(),
                invoice.getFinalTotal(),
                invoice.getRefundedTotal(),
                invoice.getCreatedBy(),
                invoice.getCreatedAt(),
                invoice.getCompletedBy(),
                invoice.getCompletedAt());
    }
}
