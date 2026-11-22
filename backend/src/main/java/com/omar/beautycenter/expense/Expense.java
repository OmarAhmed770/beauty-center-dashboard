package com.omar.beautycenter.expense;

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
@Table(name = "expenses")
public class Expense extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(name = "category_name_arabic", nullable = false, length = 150)
    private String categoryNameArabic;

    @Column(name = "category_name_english", nullable = false, length = 150)
    private String categoryNameEnglish;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_method_code", nullable = false, length = 50)
    private String paymentMethodCode;

    @Column(name = "payment_method_name_arabic", nullable = false, length = 150)
    private String paymentMethodNameArabic;

    @Column(name = "payment_method_name_english", nullable = false, length = 150)
    private String paymentMethodNameEnglish;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", nullable = false, length = 20)
    private PaymentMethodType paymentMethodType;

    @Column(name = "payment_reference", length = 200)
    private String paymentReference;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "receipt_path", length = 500)
    private String receiptPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Expense() {
    }

    public Expense(
            ExpenseCategory category,
            BigDecimal amount,
            LocalDate businessDate,
            PaymentMethod paymentMethod,
            String paymentReference,
            String description,
            String notes,
            String receiptPath) {
        this.category = category;
        this.categoryNameArabic = category.getNameArabic();
        this.categoryNameEnglish = category.getNameEnglish();
        this.amount = amount;
        this.businessDate = businessDate;
        this.paymentMethod = paymentMethod;
        this.paymentMethodCode = paymentMethod.getCode();
        this.paymentMethodNameArabic = paymentMethod.getDisplayNameArabic();
        this.paymentMethodNameEnglish = paymentMethod.getDisplayNameEnglish();
        this.paymentMethodType = paymentMethod.getType();
        this.paymentReference = paymentReference;
        this.description = description;
        this.notes = notes;
        this.receiptPath = receiptPath;
        this.status = ExpenseStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategoryNameArabic() {
        return categoryNameArabic;
    }

    public String getCategoryNameEnglish() {
        return categoryNameEnglish;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentMethodCode() {
        return paymentMethodCode;
    }

    public String getPaymentMethodNameArabic() {
        return paymentMethodNameArabic;
    }

    public String getPaymentMethodNameEnglish() {
        return paymentMethodNameEnglish;
    }

    public PaymentMethodType getPaymentMethodType() {
        return paymentMethodType;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getDescription() {
        return description;
    }

    public String getNotes() {
        return notes;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public ExpenseStatus getStatus() {
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
        this.status = ExpenseStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = cancellationReason;
    }
}
