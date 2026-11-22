package com.omar.beautycenter.closing;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
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
@Table(name = "cash_adjustments")
public class CashAdjustment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 20, updatable = false)
    private CashAdjustmentType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, length = 500, updatable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CashAdjustmentStatus status;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CashAdjustment() { }

    public CashAdjustment(LocalDate businessDate, CashAdjustmentType type, BigDecimal amount, String reason) {
        this.businessDate = businessDate;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.status = CashAdjustmentStatus.ACTIVE;
    }

    public void cancel(String actor, LocalDateTime at, String reason) {
        this.status = CashAdjustmentStatus.CANCELLED;
        this.cancelledBy = actor;
        this.cancelledAt = at;
        this.cancellationReason = reason;
    }

    public Long getId() { return id; }
    public LocalDate getBusinessDate() { return businessDate; }
    public CashAdjustmentType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public CashAdjustmentStatus getStatus() { return status; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public long getVersion() { return version; }
}
