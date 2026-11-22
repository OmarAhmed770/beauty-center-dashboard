package com.omar.beautycenter.discount;

import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.common.error.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class DiscountCalculationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public DiscountCalculation calculate(
            BigDecimal originalAmount,
            Discount discount,
            LocalDate applicationDate,
            CatalogItem catalogItem) {
        BigDecimal normalizedAmount = money(originalAmount, "Original amount");
        if (discount == null) {
            return new DiscountCalculation(normalizedAmount, money(BigDecimal.ZERO, "Discount amount"), normalizedAmount);
        }
        if (applicationDate == null) {
            throw new BusinessRuleException("Discount application date is required.");
        }
        if (!discount.isActive()
                || applicationDate.isBefore(discount.getStartDate())
                || applicationDate.isAfter(discount.getEndDate())) {
            throw new BusinessRuleException("Inactive, not-yet-active, or expired discounts cannot be applied.");
        }
        assertApplicable(discount, catalogItem);

        BigDecimal discountAmount;
        if (discount.getType() == DiscountType.PERCENTAGE) {
            discountAmount = normalizedAmount
                    .multiply(discount.getValue())
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = discount.getValue().setScale(2, RoundingMode.UNNECESSARY);
            if (discountAmount.compareTo(normalizedAmount) > 0) {
                throw new BusinessRuleException("A fixed discount cannot create a negative final price.");
            }
        }
        return new DiscountCalculation(
                normalizedAmount,
                discountAmount,
                normalizedAmount.subtract(discountAmount).setScale(2, RoundingMode.UNNECESSARY));
    }

    private static void assertApplicable(Discount discount, CatalogItem catalogItem) {
        if (discount.getCatalogItem() != null
                && (catalogItem == null
                        || !discount.getCatalogItem().getId().equals(catalogItem.getId()))) {
            throw new BusinessRuleException("The discount does not apply to this catalog item.");
        }
        if (discount.getCategory() != null
                && (catalogItem == null
                        || !discount.getCategory().getId().equals(catalogItem.getCategory().getId()))) {
            throw new BusinessRuleException("The discount does not apply to this catalog category.");
        }
    }

    private static BigDecimal money(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new BusinessRuleException(field + " must be zero or greater.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException(field + " must have at most 2 decimal digits.");
        }
    }
}
