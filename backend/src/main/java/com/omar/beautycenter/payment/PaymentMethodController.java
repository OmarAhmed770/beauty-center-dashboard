package com.omar.beautycenter.payment;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.payment.dto.CreatePaymentMethodRequest;
import com.omar.beautycenter.payment.dto.PaymentMethodActiveRequest;
import com.omar.beautycenter.payment.dto.PaymentMethodResponse;
import com.omar.beautycenter.payment.dto.UpdatePaymentMethodRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/payment-methods")
@Tag(name = "Payment Methods")
@SecurityRequirement(name = "bearerAuth")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a payment method", description = "Requires the Settings permission")
    public PaymentMethodResponse create(@Valid @RequestBody CreatePaymentMethodRequest request) {
        return paymentMethodService.create(request);
    }

    @PutMapping("/{paymentMethodId}")
    @Operation(summary = "Update a payment method", description = "Requires the Settings permission; code is immutable")
    public PaymentMethodResponse update(
            @PathVariable @Positive Long paymentMethodId,
            @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return paymentMethodService.update(paymentMethodId, request);
    }

    @PatchMapping("/{paymentMethodId}/active")
    @Operation(summary = "Activate or deactivate a payment method", description = "Requires the Settings permission")
    public PaymentMethodResponse setActive(
            @PathVariable @Positive Long paymentMethodId,
            @Valid @RequestBody PaymentMethodActiveRequest request) {
        return paymentMethodService.setActive(paymentMethodId, request.active());
    }

    @GetMapping("/{paymentMethodId}")
    @Operation(summary = "Get a payment method", description = "Available to all authenticated roles")
    public PaymentMethodResponse getById(@PathVariable @Positive Long paymentMethodId) {
        return paymentMethodService.getById(paymentMethodId);
    }

    @GetMapping
    @Operation(summary = "Search payment methods", description = "Available to all authenticated roles")
    public PageResponse<PaymentMethodResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) PaymentMethodType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return paymentMethodService.search(query, active, type, page, size);
    }
}
