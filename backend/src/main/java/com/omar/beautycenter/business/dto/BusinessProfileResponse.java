package com.omar.beautycenter.business.dto;

import com.omar.beautycenter.business.BusinessCurrency;
import com.omar.beautycenter.business.BusinessProfile;
import java.time.LocalDateTime;

public record BusinessProfileResponse(
        String businessName,
        String logoDataUrl,
        String phone,
        String address,
        BusinessCurrency currency,
        String receiptInformation,
        LocalDateTime updatedAt) {

    public static BusinessProfileResponse from(BusinessProfile profile) {
        return new BusinessProfileResponse(
                profile.getBusinessName(),
                profile.getLogoDataUrl(),
                profile.getPhone(),
                profile.getAddress(),
                profile.getCurrency(),
                profile.getReceiptInformation(),
                profile.getUpdatedAt());
    }
}
