package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.catalog.CatalogItemType;
import com.omar.beautycenter.discount.DiscountType;
import com.omar.beautycenter.sales.InvoiceItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceItemResponse(
        Long id,
        Long catalogItemId,
        boolean manualItem,
        Long hairConfigurationId,
        String itemCode,
        String itemNameArabic,
        String itemNameEnglish,
        CatalogItemType itemType,
        BigDecimal quantity,
        BigDecimal unitOriginalSellingPrice,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal unitCostSnapshot,
        BigDecimal installationFeeSnapshot,
        Long discountId,
        String discountName,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal lineSubtotal,
        BigDecimal finalLineTotal,
        Long employeeId,
        String employeeName,
        boolean priceOverridden,
        BigDecimal priceOverrideOriginalAmount,
        BigDecimal priceOverrideAmount,
        String priceOverrideReason,
        String priceOverriddenBy,
        LocalDateTime priceOverriddenAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static InvoiceItemResponse from(InvoiceItem item, boolean includeCost) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getCatalogItem() == null ? null : item.getCatalogItem().getId(),
                item.isManualItem(),
                item.getHairConfiguration() == null ? null : item.getHairConfiguration().getId(),
                item.getItemCodeSnapshot(),
                item.getItemNameArabicSnapshot(),
                item.getItemNameEnglishSnapshot(),
                item.getItemTypeSnapshot(),
                item.getQuantity(),
                item.getUnitOriginalSellingPrice(),
                includeCost ? item.getUnitCostSnapshot() : null,
                item.getInstallationFeeSnapshot(),
                item.getDiscount() == null ? null : item.getDiscount().getId(),
                item.getDiscountNameSnapshot(),
                item.getDiscountTypeSnapshot(),
                item.getDiscountValueSnapshot(),
                item.getDiscountAmount(),
                item.getLineSubtotal(),
                item.getFinalLineTotal(),
                item.getEmployee() == null ? null : item.getEmployee().getId(),
                item.getEmployeeNameSnapshot(),
                item.getPriceOverrideAmount() != null,
                item.getPriceOverrideOriginalAmount(),
                item.getPriceOverrideAmount(),
                item.getPriceOverrideReason(),
                item.getPriceOverriddenBy(),
                item.getPriceOverriddenAt(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getCreatedBy(),
                item.getUpdatedBy());
    }
}
