package com.omar.beautycenter.business.dto;

import com.omar.beautycenter.business.BusinessProfile;

public record PublicBusinessIdentityResponse(
        String businessName,
        String logoDataUrl) {

    public static PublicBusinessIdentityResponse from(BusinessProfile profile) {
        return new PublicBusinessIdentityResponse(profile.getBusinessName(), profile.getLogoDataUrl());
    }
}
