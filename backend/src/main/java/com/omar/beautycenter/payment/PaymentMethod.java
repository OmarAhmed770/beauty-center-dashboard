package com.omar.beautycenter.payment;

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

@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50, updatable = false)
    private String code;

    @Column(name = "display_name_arabic", nullable = false, length = 150)
    private String displayNameArabic;

    @Column(name = "display_name_english", nullable = false, length = 150)
    private String displayNameEnglish;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentMethodType type;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "requires_reference_number", nullable = false)
    private boolean requiresReferenceNumber;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PaymentMethod() {
    }

    public PaymentMethod(
            String code,
            String displayNameArabic,
            String displayNameEnglish,
            PaymentMethodType type,
            boolean active,
            boolean requiresReferenceNumber,
            int sortOrder) {
        this.code = code;
        this.displayNameArabic = displayNameArabic;
        this.displayNameEnglish = displayNameEnglish;
        this.type = type;
        this.active = active;
        this.requiresReferenceNumber = requiresReferenceNumber;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayNameArabic() {
        return displayNameArabic;
    }

    public String getDisplayNameEnglish() {
        return displayNameEnglish;
    }

    public PaymentMethodType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isRequiresReferenceNumber() {
        return requiresReferenceNumber;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public long getVersion() {
        return version;
    }

    public void update(
            String displayNameArabic,
            String displayNameEnglish,
            PaymentMethodType type,
            boolean requiresReferenceNumber,
            int sortOrder) {
        this.displayNameArabic = displayNameArabic;
        this.displayNameEnglish = displayNameEnglish;
        this.type = type;
        this.requiresReferenceNumber = requiresReferenceNumber;
        this.sortOrder = sortOrder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
