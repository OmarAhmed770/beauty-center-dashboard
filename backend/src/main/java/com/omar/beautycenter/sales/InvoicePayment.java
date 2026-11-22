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
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_payments")
public class InvoicePayment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false, updatable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_method_code_snapshot", nullable = false, length = 50)
    private String paymentMethodCodeSnapshot;

    @Column(name = "payment_method_name_arabic_snapshot", nullable = false, length = 150)
    private String paymentMethodNameArabicSnapshot;

    @Column(name = "payment_method_name_english_snapshot", nullable = false, length = 150)
    private String paymentMethodNameEnglishSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type_snapshot", nullable = false, length = 20)
    private PaymentMethodType paymentMethodTypeSnapshot;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_number", length = 200)
    private String referenceNumber;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InvoicePayment() {
    }

    public InvoicePayment(
            Invoice invoice,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String referenceNumber) {
        this.invoice = invoice;
        this.paymentMethod = paymentMethod;
        this.paymentMethodCodeSnapshot = paymentMethod.getCode();
        this.paymentMethodNameArabicSnapshot = paymentMethod.getDisplayNameArabic();
        this.paymentMethodNameEnglishSnapshot = paymentMethod.getDisplayNameEnglish();
        this.paymentMethodTypeSnapshot = paymentMethod.getType();
        this.amount = amount;
        this.referenceNumber = referenceNumber;
    }

    public Long getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getPaymentMethodCodeSnapshot() { return paymentMethodCodeSnapshot; }
    public String getPaymentMethodNameArabicSnapshot() { return paymentMethodNameArabicSnapshot; }
    public String getPaymentMethodNameEnglishSnapshot() { return paymentMethodNameEnglishSnapshot; }
    public PaymentMethodType getPaymentMethodTypeSnapshot() { return paymentMethodTypeSnapshot; }
    public BigDecimal getAmount() { return amount; }
    public String getReferenceNumber() { return referenceNumber; }
    public long getVersion() { return version; }
}
