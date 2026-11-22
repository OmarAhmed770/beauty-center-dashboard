package com.omar.beautycenter.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ExpenseDomainTest {

    @Test
    void cancellationChangesOnlyLifecycleAndPreservesFinancialValues() {
        ExpenseCategory category = new ExpenseCategory("صيانة", "Maintenance", true, null);
        PaymentMethod method = new PaymentMethod(
                "CASH", "نقدي", "Cash", PaymentMethodType.CASH, true, false, 10);
        Expense expense = new Expense(
                category,
                new BigDecimal("125.50"),
                LocalDate.of(2026, 7, 26),
                method,
                null,
                "Repair",
                null,
                null);
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 7, 26, 12, 30);

        expense.cancel(cancelledAt, "owner", "Duplicate entry");

        assertEquals(ExpenseStatus.CANCELLED, expense.getStatus());
        assertEquals(cancelledAt, expense.getCancelledAt());
        assertEquals("owner", expense.getCancelledBy());
        assertEquals("Duplicate entry", expense.getCancellationReason());
        assertEquals(new BigDecimal("125.50"), expense.getAmount());
        assertEquals(LocalDate.of(2026, 7, 26), expense.getBusinessDate());
        assertNull(expense.getPaymentReference());
    }
}
