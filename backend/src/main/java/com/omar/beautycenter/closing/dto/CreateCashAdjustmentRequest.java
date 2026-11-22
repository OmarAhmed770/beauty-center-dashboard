package com.omar.beautycenter.closing.dto;

import com.omar.beautycenter.closing.CashAdjustmentType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCashAdjustmentRequest(
        @NotNull LocalDate businessDate,
        @NotNull CashAdjustmentType type,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 500) String reason) {
}
