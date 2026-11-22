package com.omar.beautycenter.employee;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodType;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_transactions")
public class EmployeeTransaction extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private EmployeeTransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethodReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", length = 20)
    private PaymentMethodType paymentMethodType;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeTransactionStatus status;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EmployeeTransaction() {
    }

    public EmployeeTransaction(
            Employee employee,
            EmployeeTransactionType type,
            BigDecimal amount,
            LocalDate businessDate,
            PaymentMethod paymentMethod,
            String notes) {
        this.employee = employee;
        this.type = type;
        this.amount = amount;
        this.businessDate = businessDate;
        this.paymentMethodReference = paymentMethod;
        this.paymentMethod = paymentMethod == null ? null : paymentMethod.getCode();
        this.paymentMethodType = paymentMethod == null ? null : paymentMethod.getType();
        this.notes = notes;
        this.status = EmployeeTransactionStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public EmployeeTransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentMethod getPaymentMethodReference() {
        return paymentMethodReference;
    }

    public PaymentMethodType getPaymentMethodType() {
        return paymentMethodType;
    }

    public String getNotes() {
        return notes;
    }

    public EmployeeTransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public long getVersion() {
        return version;
    }

    public void cancel(LocalDateTime cancelledAt, String cancelledBy, String cancellationReason) {
        this.status = EmployeeTransactionStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = cancellationReason;
    }
}
