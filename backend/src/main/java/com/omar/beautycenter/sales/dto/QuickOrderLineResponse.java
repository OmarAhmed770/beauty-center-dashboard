package com.omar.beautycenter.sales.dto;

import com.omar.beautycenter.sales.InvoiceItem;
import java.math.BigDecimal;

/** Deliberately excludes cost and profit information. */
public record QuickOrderLineResponse(
        Long catalogItemId,
        String itemNameArabic,
        BigDecimal quantity,
        BigDecimal finalLineTotal,
        Long employeeId,
        String employeeName) {

    public static QuickOrderLineResponse from(InvoiceItem item) {
        return new QuickOrderLineResponse(
                item.getCatalogItem() == null ? null : item.getCatalogItem().getId(),
                item.getItemNameArabicSnapshot(),
                item.getQuantity(),
                item.getFinalLineTotal(),
                item.getEmployee() == null ? null : item.getEmployee().getId(),
                item.getEmployeeNameSnapshot());
    }
}
