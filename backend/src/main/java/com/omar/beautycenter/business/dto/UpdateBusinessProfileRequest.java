package com.omar.beautycenter.business.dto;

import com.omar.beautycenter.business.BusinessCurrency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBusinessProfileRequest(
        @NotBlank @Size(max = 150) String businessName,
        @Size(max = 350000) String logoDataUrl,
        @Size(max = 50) String phone,
        @Size(max = 500) String address,
        @NotNull BusinessCurrency currency,
        @Size(max = 1000) String receiptInformation) {
}
