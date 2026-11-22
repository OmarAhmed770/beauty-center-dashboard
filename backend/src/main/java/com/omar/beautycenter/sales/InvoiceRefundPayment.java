package com.omar.beautycenter.sales;

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
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_refund_payments")
public class InvoiceRefundPayment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false, updatable = false)
    private InvoiceRefund refund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false, updatable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_method_code_snapshot", nullable = false, length = 50, updatable = false)
    private String paymentMethodCodeSnapshot;

    @Column(name = "payment_method_name_arabic_snapshot", nullable = false, length = 150, updatable = false)
    private String paymentMethodNameArabicSnapshot;

    @Column(name = "payment_method_name_english_snapshot", nullable = false, length = 150, updatable = false)
    private String paymentMethodNameEnglishSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type_snapshot", nullable = false, length = 20, updatable = false)
    private PaymentMethodType paymentMethodTypeSnapshot;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "reference_number", length = 200, updatable = false)
    private String referenceNumber;

    protected InvoiceRefundPayment() {
    }

    public InvoiceRefundPayment(
            InvoiceRefund refund,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String referenceNumber) {
        this.refund = refund;
        this.paymentMethod = paymentMethod;
        this.paymentMethodCodeSnapshot = paymentMethod.getCode();
        this.paymentMethodNameArabicSnapshot = paymentMethod.getDisplayNameArabic();
        this.paymentMethodNameEnglishSnapshot = paymentMethod.getDisplayNameEnglish();
        this.paymentMethodTypeSnapshot = paymentMethod.getType();
        this.amount = amount;
        this.referenceNumber = referenceNumber;
    }

    public Long getId() { return id; }
    public InvoiceRefund getRefund() { return refund; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getPaymentMethodCodeSnapshot() { return paymentMethodCodeSnapshot; }
    public String getPaymentMethodNameArabicSnapshot() { return paymentMethodNameArabicSnapshot; }
    public String getPaymentMethodNameEnglishSnapshot() { return paymentMethodNameEnglishSnapshot; }
    public PaymentMethodType getPaymentMethodTypeSnapshot() { return paymentMethodTypeSnapshot; }
    public BigDecimal getAmount() { return amount; }
    public String getReferenceNumber() { return referenceNumber; }
}
