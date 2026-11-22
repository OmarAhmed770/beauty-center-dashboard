package com.omar.beautycenter.sales;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import com.omar.beautycenter.common.error.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseAuditEntity {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, updatable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "final_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalTotal;

    @Column(name = "refunded_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedTotal;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "client_request_id", unique = true, length = 100, updatable = false)
    private String clientRequestId;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Invoice() {
    }

    public Invoice(
            String invoiceNumber,
            LocalDate businessDate,
            String customerName,
            String customerPhone,
            String notes) {
        this(invoiceNumber, businessDate, customerName, customerPhone, notes, null);
    }

    public Invoice(
            String invoiceNumber,
            LocalDate businessDate,
            String customerName,
            String customerPhone,
            String notes,
            String clientRequestId) {
        this.invoiceNumber = invoiceNumber;
        this.businessDate = businessDate;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.notes = notes;
        this.clientRequestId = clientRequestId;
        this.status = InvoiceStatus.DRAFT;
        this.subtotal = ZERO;
        this.discountTotal = ZERO;
        this.finalTotal = ZERO;
        this.refundedTotal = ZERO;
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public BigDecimal getRefundedTotal() {
        return refundedTotal;
    }

    public String getNotes() {
        return notes;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public long getVersion() {
        return version;
    }

    public void updateTotals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal finalTotal) {
        assertDraft();
        this.subtotal = subtotal;
        this.discountTotal = discountTotal;
        this.finalTotal = finalTotal;
    }

    public void complete(String actor, LocalDateTime at) {
        assertDraft();
        this.status = InvoiceStatus.COMPLETED;
        this.completedBy = actor;
        this.completedAt = at;
    }

    public void cancel(String actor, LocalDateTime at, String reason) {
        if (status == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("The invoice is already cancelled.");
        }
        if (status == InvoiceStatus.PARTIALLY_REFUNDED || status == InvoiceStatus.REFUNDED) {
            throw new BusinessRuleException("Refunded invoices cannot be cancelled.");
        }
        this.status = InvoiceStatus.CANCELLED;
        this.cancelledBy = actor;
        this.cancelledAt = at;
        this.cancellationReason = reason;
    }

    public void registerRefund(BigDecimal amount) {
        if (status != InvoiceStatus.COMPLETED && status != InvoiceStatus.PARTIALLY_REFUNDED) {
            throw new BusinessRuleException("Only completed invoices can be refunded.");
        }
        BigDecimal newTotal = refundedTotal.add(amount);
        if (newTotal.compareTo(finalTotal) > 0) {
            throw new BusinessRuleException("Refund amount exceeds the remaining refundable amount.");
        }
        refundedTotal = newTotal;
        status = newTotal.compareTo(finalTotal) == 0
                ? InvoiceStatus.REFUNDED
                : InvoiceStatus.PARTIALLY_REFUNDED;
    }

    public void assertDraft() {
        if (status != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only draft invoices may be edited.");
        }
    }
}
