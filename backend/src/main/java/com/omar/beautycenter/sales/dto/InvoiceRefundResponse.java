package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.sales.InvoiceRefund;
import com.omar.beautycenter.sales.InvoiceRefundPayment;
import com.omar.beautycenter.sales.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceRefundResponse(
        Long id,
        Long invoiceId,
        LocalDate businessDate,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        List<InvoiceRefundPaymentResponse> payments,
        LocalDateTime createdAt,
        String createdBy) {

    public static InvoiceRefundResponse from(
            InvoiceRefund refund,
            List<InvoiceRefundPayment> payments) {
        return new InvoiceRefundResponse(
                refund.getId(),
                refund.getInvoice().getId(),
                refund.getBusinessDate(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                payments.stream().map(InvoiceRefundPaymentResponse::from).toList(),
                refund.getCreatedAt(),
                refund.getCreatedBy());
    }
}
