package com.omar.beautycenter.business;

import com.omar.beautycenter.common.audit.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "business_profile")
public class BusinessProfile extends BaseAuditEntity {

    @Id
    private Long id;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "logo_data_url", columnDefinition = "TEXT")
    private String logoDataUrl;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private BusinessCurrency currency;

    @Column(name = "receipt_information", length = 1000)
    private String receiptInformation;

    @Version
    private long version;

    protected BusinessProfile() {
    }

    public BusinessProfile(
            Long id,
            String businessName,
            String logoDataUrl,
            String phone,
            String address,
            BusinessCurrency currency,
            String receiptInformation) {
        this.id = id;
        update(businessName, logoDataUrl, phone, address, currency, receiptInformation);
    }

    public void update(
            String businessName,
            String logoDataUrl,
            String phone,
            String address,
            BusinessCurrency currency,
            String receiptInformation) {
        this.businessName = businessName;
        this.logoDataUrl = logoDataUrl;
        this.phone = phone;
        this.address = address;
        this.currency = currency;
        this.receiptInformation = receiptInformation;
    }

    public Long getId() { return id; }
    public String getBusinessName() { return businessName; }
    public String getLogoDataUrl() { return logoDataUrl; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public BusinessCurrency getCurrency() { return currency; }
    public String getReceiptInformation() { return receiptInformation; }
}
