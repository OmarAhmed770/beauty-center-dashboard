package com.omar.beautycenter.discount;

import java.math.BigDecimal;

public record DiscountCalculation(
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount) {
}
