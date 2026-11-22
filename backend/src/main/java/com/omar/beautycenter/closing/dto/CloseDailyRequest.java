package com.omar.beautycenter.closing.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CloseDailyRequest(
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal actualCash,
        @Size(max = 2000) String notes) {
}
