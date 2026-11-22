package com.omar.beautycenter.payment.dto;

import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodType;
import java.time.LocalDateTime;

public record PaymentMethodResponse(
        Long id,
        String code,
        String displayNameArabic,
        String displayNameEnglish,
        PaymentMethodType type,
        boolean active,
        boolean requiresReferenceNumber,
        int sortOrder,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public static PaymentMethodResponse from(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.getId(),
                method.getCode(),
                method.getDisplayNameArabic(),
                method.getDisplayNameEnglish(),
                method.getType(),
                method.isActive(),
                method.isRequiresReferenceNumber(),
                method.getSortOrder(),
                method.getVersion(),
                method.getCreatedAt(),
                method.getUpdatedAt(),
                method.getCreatedBy(),
                method.getUpdatedBy());
    }
}
