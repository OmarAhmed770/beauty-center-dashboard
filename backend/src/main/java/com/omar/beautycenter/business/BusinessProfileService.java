package com.omar.beautycenter.business;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.business.dto.BusinessProfileResponse;
import com.omar.beautycenter.business.dto.PublicBusinessIdentityResponse;
import com.omar.beautycenter.business.dto.UpdateBusinessProfileRequest;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessProfileService {

    private static final long PROFILE_ID = 1L;
    private static final Pattern IMAGE_DATA_URL = Pattern.compile(
            "^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=\\r\\n]+$");

    private final BusinessProfileRepository repository;
    private final AuditLogService auditLogService;

    public BusinessProfileService(BusinessProfileRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public BusinessProfileResponse get() {
        return BusinessProfileResponse.from(getProfile());
    }

    @Transactional(readOnly = true)
    public PublicBusinessIdentityResponse getPublicIdentity() {
        return PublicBusinessIdentityResponse.from(getProfile());
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public BusinessProfileResponse update(UpdateBusinessProfileRequest request) {
        return updateProfile(request, "BUSINESS_PROFILE_UPDATED");
    }

    /** Called only from the one-time setup transaction before an authenticated session exists. */
    @Transactional
    public BusinessProfileResponse configureInitialSetup(String businessName, BusinessCurrency currency) {
        BusinessProfile profile = getProfile();
        profile.update(
                required(businessName, "Business name", 150),
                profile.getLogoDataUrl(),
                profile.getPhone(),
                profile.getAddress(),
                currency,
                profile.getReceiptInformation());
        BusinessProfile saved = repository.saveAndFlush(profile);
        auditLogService.record(
                "INITIAL_BUSINESS_PROFILE_CONFIGURED",
                "BUSINESS_PROFILE",
                PROFILE_ID,
                null,
                Map.of("businessName", saved.getBusinessName(), "currency", saved.getCurrency()));
        return BusinessProfileResponse.from(saved);
    }

    private BusinessProfileResponse updateProfile(UpdateBusinessProfileRequest request, String auditAction) {
        BusinessProfile profile = getProfile();
        profile.update(
                required(request.businessName(), "Business name", 150),
                logo(request.logoDataUrl()),
                optional(request.phone(), "Phone", 50),
                optional(request.address(), "Address", 500),
                request.currency(),
                optional(request.receiptInformation(), "Receipt information", 1000));
        BusinessProfile saved = repository.saveAndFlush(profile);
        auditLogService.record(
                auditAction,
                "BUSINESS_PROFILE",
                PROFILE_ID,
                null,
                Map.of("businessName", saved.getBusinessName(), "currency", saved.getCurrency()));
        return BusinessProfileResponse.from(saved);
    }

    private BusinessProfile getProfile() {
        return repository.findById(PROFILE_ID)
                .orElseThrow(() -> new NotFoundException("Business profile is not initialized."));
    }

    private static String required(String value, String field, int maxLength) {
        String normalized = optional(value, field, maxLength);
        if (normalized == null) {
            throw new BusinessRuleException(field + " is required.");
        }
        return normalized;
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new BusinessRuleException(field + " must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private static String logo(String value) {
        String normalized = optional(value, "Logo", 350000);
        if (normalized != null && !IMAGE_DATA_URL.matcher(normalized).matches()) {
            throw new BusinessRuleException("Logo must be a PNG, JPEG, or WebP image.");
        }
        return normalized;
    }
}
