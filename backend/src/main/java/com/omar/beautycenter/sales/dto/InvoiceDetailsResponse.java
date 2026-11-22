package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.sales.Invoice;
import com.omar.beautycenter.sales.InvoiceItem;
import com.omar.beautycenter.sales.InvoicePayment;
import com.omar.beautycenter.sales.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDetailsResponse(
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
        String notes,
        List<InvoiceItemResponse> items,
        List<InvoicePaymentResponse> payments,
        List<InvoiceRefundResponse> refunds,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        String completedBy,
        LocalDateTime completedAt,
        String cancelledBy,
        LocalDateTime cancelledAt,
        String cancellationReason) {

    public static InvoiceDetailsResponse from(
            Invoice invoice,
            List<InvoiceItem> items,
            List<InvoicePayment> payments,
            List<InvoiceRefundResponse> refunds,
            boolean includeCost) {
        return new InvoiceDetailsResponse(
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
                invoice.getNotes(),
                items.stream().map(item -> InvoiceItemResponse.from(item, includeCost)).toList(),
                payments.stream().map(InvoicePaymentResponse::from).toList(),
                List.copyOf(refunds),
                invoice.getCreatedBy(),
                invoice.getCreatedAt(),
                invoice.getUpdatedBy(),
                invoice.getUpdatedAt(),
                invoice.getCompletedBy(),
                invoice.getCompletedAt(),
                invoice.getCancelledBy(),
                invoice.getCancelledAt(),
                invoice.getCancellationReason());
    }
}
