package com.omar.beautycenter.catalog.dto;

import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.catalog.HairProductStock;
import java.math.BigDecimal;

public record HairStockResponse(
        Long hairItemId,
        String hairItemCode,
        String hairItemNameArabic,
        boolean stockConfigured,
        BigDecimal availableGrams,
        long version) {

    public static HairStockResponse unconfigured(CatalogItem item) {
        return new HairStockResponse(item.getId(), item.getCode(), item.getNameArabic(), false, null, 0);
    }

    public static HairStockResponse from(HairProductStock stock) {
        CatalogItem item = stock.getHairItem();
        return new HairStockResponse(
                item.getId(), item.getCode(), item.getNameArabic(), true,
                stock.getAvailableGrams(), stock.getVersion());
    }
}
