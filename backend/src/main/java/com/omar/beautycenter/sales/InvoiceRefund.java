package com.omar.beautycenter.sales;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_refunds")
public class InvoiceRefund extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, length = 500, updatable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, updatable = false)
    private RefundStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InvoiceRefund() {
    }

    public InvoiceRefund(Invoice invoice, LocalDate businessDate, BigDecimal amount, String reason) {
        this.invoice = invoice;
        this.businessDate = businessDate;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.COMPLETED;
    }

    public Long getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public LocalDate getBusinessDate() { return businessDate; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public RefundStatus getStatus() { return status; }
    public long getVersion() { return version; }
}
