package com.omar.beautycenter.closing;

import com.omar.beautycenter.payment.PaymentMethodType;
import com.omar.beautycenter.report.dto.PaymentMethodFinancialTotalResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "daily_closing_payment_totals")
public class DailyClosingPaymentTotal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "closing_id", nullable = false)
    private DailyClosing closing;
    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;
    @Column(name = "payment_method_code_snapshot", nullable = false, length = 50)
    private String code;
    @Column(name = "payment_method_name_arabic_snapshot", nullable = false, length = 150)
    private String nameArabic;
    @Column(name = "payment_method_name_english_snapshot", nullable = false, length = 150)
    private String nameEnglish;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method_type_snapshot", nullable = false, length = 20)
    private PaymentMethodType type;
    @Column(name = "received_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal receivedAmount;
    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;
    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    protected DailyClosingPaymentTotal() { }

    public DailyClosingPaymentTotal(DailyClosing closing, PaymentMethodFinancialTotalResponse total) {
        this.closing = closing; this.paymentMethodId = total.paymentMethodId(); this.code = total.code();
        this.nameArabic = total.displayNameArabic(); this.nameEnglish = total.displayNameEnglish();
        this.type = total.type(); this.receivedAmount = total.receivedAmount();
        this.refundedAmount = total.refundedAmount(); this.netAmount = total.netAmount();
    }

    public Long getPaymentMethodId() { return paymentMethodId; }
    public String getCode() { return code; }
    public String getNameArabic() { return nameArabic; }
    public String getNameEnglish() { return nameEnglish; }
    public PaymentMethodType getType() { return type; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
}
