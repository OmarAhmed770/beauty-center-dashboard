package com.omar.beautycenter.report.dto;

import com.omar.beautycenter.catalog.CatalogItemType;
import java.math.BigDecimal;

public record TopCatalogItemResponse(
        Long catalogItemId,
        String itemCode,
        String itemNameArabic,
        String itemNameEnglish,
        CatalogItemType itemType,
        BigDecimal quantity,
        BigDecimal netRevenue,
        long invoiceCount) {
}
