package com.omar.beautycenter.sales;

import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.catalog.CatalogItemType;
import com.omar.beautycenter.catalog.HairConfiguration;
import com.omar.beautycenter.common.audit.BaseAuditEntity;
import com.omar.beautycenter.discount.Discount;
import com.omar.beautycenter.discount.DiscountType;
import com.omar.beautycenter.employee.Employee;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hair_configuration_id")
    private HairConfiguration hairConfiguration;

    @Column(name = "item_code_snapshot", nullable = false, length = 50)
    private String itemCodeSnapshot;

    @Column(name = "item_name_arabic_snapshot", nullable = false, length = 150)
    private String itemNameArabicSnapshot;

    @Column(name = "item_name_english_snapshot", nullable = false, length = 150)
    private String itemNameEnglishSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type_snapshot", nullable = false, length = 30)
    private CatalogItemType itemTypeSnapshot;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_original_selling_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitOriginalSellingPrice;

    @Column(name = "unit_cost_snapshot", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCostSnapshot;

    @Column(name = "installation_fee_snapshot", nullable = false, precision = 19, scale = 2)
    private BigDecimal installationFeeSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @Column(name = "discount_name_snapshot", length = 150)
    private String discountNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type_snapshot", length = 20)
    private DiscountType discountTypeSnapshot;

    @Column(name = "discount_value_snapshot", precision = 19, scale = 2)
    private BigDecimal discountValueSnapshot;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineSubtotal;

    @Column(name = "final_line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalLineTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "employee_name_snapshot", length = 150)
    private String employeeNameSnapshot;

    @Column(name = "price_override_original_amount", precision = 19, scale = 2)
    private BigDecimal priceOverrideOriginalAmount;

    @Column(name = "price_override_amount", precision = 19, scale = 2)
    private BigDecimal priceOverrideAmount;

    @Column(name = "price_override_reason", length = 500)
    private String priceOverrideReason;

    @Column(name = "price_overridden_by", length = 100)
    private String priceOverriddenBy;

    @Column(name = "price_overridden_at")
    private LocalDateTime priceOverriddenAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InvoiceItem() {
    }

    public InvoiceItem(Invoice invoice, InvoiceItemSnapshot snapshot) {
        this.invoice = invoice;
        apply(snapshot);
    }

    public void update(InvoiceItemSnapshot snapshot) {
        invoice.assertDraft();
        apply(snapshot);
    }

    private void apply(InvoiceItemSnapshot snapshot) {
        this.catalogItem = snapshot.catalogItem();
        this.hairConfiguration = snapshot.hairConfiguration();
        this.itemCodeSnapshot = snapshot.itemCode();
        this.itemNameArabicSnapshot = snapshot.itemNameArabic();
        this.itemNameEnglishSnapshot = snapshot.itemNameEnglish();
        this.itemTypeSnapshot = snapshot.itemType();
        this.quantity = snapshot.quantity();
        this.unitOriginalSellingPrice = snapshot.unitSellingPrice();
        this.unitCostSnapshot = snapshot.unitCost();
        this.installationFeeSnapshot = snapshot.installationFee();
        this.discount = snapshot.discount();
        this.discountNameSnapshot = snapshot.discount() == null ? null : snapshot.discount().getName();
        this.discountTypeSnapshot = snapshot.discount() == null ? null : snapshot.discount().getType();
        this.discountValueSnapshot = snapshot.discount() == null ? null : snapshot.discount().getValue();
        this.discountAmount = snapshot.discountAmount();
        this.lineSubtotal = snapshot.lineSubtotal();
        this.finalLineTotal = snapshot.finalLineTotal();
        this.employee = snapshot.employee();
        this.employeeNameSnapshot = snapshot.employee() == null ? null : snapshot.employee().getName();
        this.priceOverrideOriginalAmount = snapshot.overrideOriginalAmount();
        this.priceOverrideAmount = snapshot.overrideAmount();
        this.priceOverrideReason = snapshot.overrideReason();
        this.priceOverriddenBy = snapshot.overriddenBy();
        this.priceOverriddenAt = snapshot.overriddenAt();
    }

    public Long getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public CatalogItem getCatalogItem() { return catalogItem; }
    public boolean isManualItem() { return catalogItem == null; }
    public HairConfiguration getHairConfiguration() { return hairConfiguration; }
    public String getItemCodeSnapshot() { return itemCodeSnapshot; }
    public String getItemNameArabicSnapshot() { return itemNameArabicSnapshot; }
    public String getItemNameEnglishSnapshot() { return itemNameEnglishSnapshot; }
    public CatalogItemType getItemTypeSnapshot() { return itemTypeSnapshot; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitOriginalSellingPrice() { return unitOriginalSellingPrice; }
    public BigDecimal getUnitCostSnapshot() { return unitCostSnapshot; }
    public BigDecimal getInstallationFeeSnapshot() { return installationFeeSnapshot; }
    public Discount getDiscount() { return discount; }
    public String getDiscountNameSnapshot() { return discountNameSnapshot; }
    public DiscountType getDiscountTypeSnapshot() { return discountTypeSnapshot; }
    public BigDecimal getDiscountValueSnapshot() { return discountValueSnapshot; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getLineSubtotal() { return lineSubtotal; }
    public BigDecimal getFinalLineTotal() { return finalLineTotal; }
    public Employee getEmployee() { return employee; }
    public String getEmployeeNameSnapshot() { return employeeNameSnapshot; }
    public BigDecimal getPriceOverrideOriginalAmount() { return priceOverrideOriginalAmount; }
    public BigDecimal getPriceOverrideAmount() { return priceOverrideAmount; }
    public String getPriceOverrideReason() { return priceOverrideReason; }
    public String getPriceOverriddenBy() { return priceOverriddenBy; }
    public LocalDateTime getPriceOverriddenAt() { return priceOverriddenAt; }
    public long getVersion() { return version; }
}
