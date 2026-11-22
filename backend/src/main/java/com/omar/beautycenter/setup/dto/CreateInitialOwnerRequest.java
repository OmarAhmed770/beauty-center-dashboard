package com.omar.beautycenter.setup.dto;

import com.omar.beautycenter.business.BusinessCurrency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateInitialOwnerRequest(
        @NotBlank(message = "Username is required.")
        @Size(max = 100, message = "Username must not exceed 100 characters.")
        String username,
        @NotBlank(message = "Display name is required.")
        @Size(max = 150, message = "Display name must not exceed 150 characters.")
        String displayName,
        @NotBlank(message = "Password is required.")
        @Size(max = 200, message = "Password must not exceed 200 characters.")
        String password,
        @Size(max = 150, message = "Business name must not exceed 150 characters.")
        String businessName,
        BusinessCurrency currency,
        List<@Valid StarterServiceRequest> starterServices,
        Boolean addDefaultExpenseCategories,
        Boolean addDefaultPaymentMethods) {

    public CreateInitialOwnerRequest(String username, String displayName, String password) {
        this(username, displayName, password, null, null, List.of(), false, false);
    }
}
