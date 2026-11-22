package com.omar.beautycenter.sales;

import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.catalog.CatalogItemType;
import com.omar.beautycenter.catalog.HairConfiguration;
import com.omar.beautycenter.discount.Discount;
import com.omar.beautycenter.employee.Employee;
import java.math.BigDecimal;
import java.time.LocalDateTime;

record InvoiceItemSnapshot(
        CatalogItem catalogItem,
        String itemCode,
        String itemNameArabic,
        String itemNameEnglish,
        CatalogItemType itemType,
        HairConfiguration hairConfiguration,
        BigDecimal quantity,
        BigDecimal unitSellingPrice,
        BigDecimal unitCost,
        BigDecimal installationFee,
        Discount discount,
        BigDecimal discountAmount,
        BigDecimal lineSubtotal,
        BigDecimal finalLineTotal,
        Employee employee,
        BigDecimal overrideOriginalAmount,
        BigDecimal overrideAmount,
        String overrideReason,
        String overriddenBy,
        LocalDateTime overriddenAt) {
}
