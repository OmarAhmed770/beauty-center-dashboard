package com.omar.beautycenter.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateEmployeeRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String name,

        @Size(max = 50, message = "Phone must not exceed 50 characters.")
        String phone,

        @NotBlank(message = "Position is required.")
        @Size(max = 100, message = "Position must not exceed 100 characters.")
        String position,

        @NotNull(message = "Default daily wage is required.")
        @DecimalMin(value = "0.00", message = "Default daily wage must not be negative.")
        @Digits(integer = 17, fraction = 2, message = "Default daily wage must have at most 17 integer and 2 decimal digits.")
        BigDecimal defaultDailyWage,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes) {
}
