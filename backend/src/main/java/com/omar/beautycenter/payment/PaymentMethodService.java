package com.omar.beautycenter.payment;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.payment.dto.CreatePaymentMethodRequest;
import com.omar.beautycenter.payment.dto.PaymentMethodResponse;
import com.omar.beautycenter.payment.dto.UpdatePaymentMethodRequest;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentMethodService {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_]{1,50}");

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public PaymentMethodResponse create(CreatePaymentMethodRequest request) {
        String code = normalizeCode(request.code());
        if (paymentMethodRepository.existsByCode(code)) {
            throw new BusinessRuleException("A payment method with this code already exists.");
        }
        PaymentMethod method = new PaymentMethod(
                code,
                requiredText(request.displayNameArabic(), "Arabic display name", 150),
                requiredText(request.displayNameEnglish(), "English display name", 150),
                requiredType(request.type()),
                Boolean.TRUE.equals(request.active()),
                Boolean.TRUE.equals(request.requiresReferenceNumber()),
                validSortOrder(request.sortOrder()));
        return PaymentMethodResponse.from(paymentMethodRepository.saveAndFlush(method));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public PaymentMethodResponse update(Long id, UpdatePaymentMethodRequest request) {
        PaymentMethod method = getEntityById(id);
        method.update(
                requiredText(request.displayNameArabic(), "Arabic display name", 150),
                requiredText(request.displayNameEnglish(), "English display name", 150),
                requiredType(request.type()),
                Boolean.TRUE.equals(request.requiresReferenceNumber()),
                validSortOrder(request.sortOrder()));
        return PaymentMethodResponse.from(paymentMethodRepository.saveAndFlush(method));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public PaymentMethodResponse setActive(Long id, boolean active) {
        PaymentMethod method = getEntityById(id);
        method.setActive(active);
        return PaymentMethodResponse.from(paymentMethodRepository.saveAndFlush(method));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('ADD_ORDERS','EXPENSES','SETTINGS','INVOICES_AND_RETURNS')")
    public PaymentMethodResponse getById(Long id) {
        return PaymentMethodResponse.from(getEntityById(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('ADD_ORDERS','EXPENSES','SETTINGS','INVOICES_AND_RETURNS')")
    public PageResponse<PaymentMethodResponse> search(
            String query,
            Boolean active,
            PaymentMethodType type,
            int page,
            int size) {
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<PaymentMethodResponse> result = paymentMethodRepository.search(
                        search,
                        active,
                        type,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("code"))))
                .map(PaymentMethodResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PaymentMethod getEntityById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment method not found."));
    }

    @Transactional(readOnly = true)
    public PaymentMethod getActiveEntityById(Long id) {
        PaymentMethod method = getEntityById(id);
        assertActive(method);
        return method;
    }

    @Transactional(readOnly = true)
    public PaymentMethod getActiveEntityByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        PaymentMethod method = paymentMethodRepository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new NotFoundException("Payment method not found."));
        assertActive(method);
        return method;
    }

    private static void assertActive(PaymentMethod method) {
        if (!method.isActive()) {
            throw new BusinessRuleException("Inactive payment methods cannot be used for new records.");
        }
    }

    private static String normalizeCode(String code) {
        String normalized = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessRuleException(
                    "Payment method code must contain 1 to 50 letters, numbers, or underscores.");
        }
        return normalized;
    }

    private static String requiredText(String value, String field, int maximumLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must contain 1 to " + maximumLength + " characters.");
        }
        return normalized;
    }

    private static PaymentMethodType requiredType(PaymentMethodType type) {
        if (type == null) {
            throw new BusinessRuleException("Payment method type is required.");
        }
        return type;
    }

    private static int validSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0 || sortOrder > 10000) {
            throw new BusinessRuleException("Sort order must be between 0 and 10000.");
        }
        return sortOrder;
    }
}
