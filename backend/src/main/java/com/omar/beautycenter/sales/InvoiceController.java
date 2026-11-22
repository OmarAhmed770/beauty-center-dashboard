package com.omar.beautycenter.sales;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.sales.dto.CancelInvoiceRequest;
import com.omar.beautycenter.sales.dto.CreateDraftInvoiceRequest;
import com.omar.beautycenter.sales.dto.CreateInvoiceRefundRequest;
import com.omar.beautycenter.sales.dto.CreateQuickOrderRequest;
import com.omar.beautycenter.sales.dto.CreateSimpleOrderRequest;
import com.omar.beautycenter.sales.dto.InvoiceDetailsResponse;
import com.omar.beautycenter.sales.dto.InvoiceRefundResponse;
import com.omar.beautycenter.sales.dto.InvoiceSummaryResponse;
import com.omar.beautycenter.sales.dto.InvoiceTotalsPreviewResponse;
import com.omar.beautycenter.sales.dto.QuickOrderResponse;
import com.omar.beautycenter.sales.dto.SetInvoicePaymentsRequest;
import com.omar.beautycenter.sales.dto.UpsertInvoiceItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/invoices")
@Tag(name = "Sales Invoices")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a draft invoice")
    public InvoiceDetailsResponse createDraft(
            @Valid @RequestBody CreateDraftInvoiceRequest request) {
        return invoiceService.createDraft(request);
    }

    @PostMapping("/simple-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create and complete a one-line manual order",
            description = "Creates a normal invoice, manual item snapshot, and one payment in one transaction. "
                    + "A reference is required only when the selected configured payment method requires it.")
    public InvoiceDetailsResponse createSimpleOrder(
            @Valid @RequestBody CreateSimpleOrderRequest request) {
        return invoiceService.createSimpleOrder(request);
    }

    @PostMapping("/quick-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create and complete a catalog-backed cashier order",
            description = "Uses normal immutable invoice snapshots and payments in one transaction. "
                    + "The client request id makes retries idempotent.")
    public QuickOrderResponse createQuickOrder(
            @Valid @RequestBody CreateQuickOrderRequest request) {
        return invoiceService.createQuickOrder(request);
    }

    @PostMapping("/{invoiceId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a calculated snapshot item to a draft invoice")
    public InvoiceDetailsResponse addItem(
            @PathVariable @Positive Long invoiceId,
            @Valid @RequestBody UpsertInvoiceItemRequest request) {
        return invoiceService.addItem(invoiceId, request);
    }

    @PutMapping("/{invoiceId}/items/{itemId}")
    @Operation(summary = "Recalculate and update an item on a draft invoice")
    public InvoiceDetailsResponse updateItem(
            @PathVariable @Positive Long invoiceId,
            @PathVariable @Positive Long itemId,
            @Valid @RequestBody UpsertInvoiceItemRequest request) {
        return invoiceService.updateItem(invoiceId, itemId, request);
    }

    @DeleteMapping("/{invoiceId}/items/{itemId}")
    @Operation(summary = "Remove an item from a draft invoice")
    public InvoiceDetailsResponse removeItem(
            @PathVariable @Positive Long invoiceId,
            @PathVariable @Positive Long itemId) {
        return invoiceService.removeItem(invoiceId, itemId);
    }

    @PutMapping("/{invoiceId}/payments")
    @Operation(summary = "Replace a draft invoice payment split")
    public InvoiceDetailsResponse setPayments(
            @PathVariable @Positive Long invoiceId,
            @Valid @RequestBody SetInvoicePaymentsRequest request) {
        return invoiceService.setPayments(invoiceId, request);
    }

    @GetMapping("/{invoiceId}/preview")
    @Operation(summary = "Preview invoice and payment totals")
    public InvoiceTotalsPreviewResponse previewTotals(
            @PathVariable @Positive Long invoiceId) {
        return invoiceService.previewTotals(invoiceId);
    }

    @PostMapping("/{invoiceId}/complete")
    @Operation(
            summary = "Complete a draft invoice transactionally",
            description = "Validates immutable snapshots and the exact payment split, then writes the completion audit record in the same transaction")
    public InvoiceDetailsResponse complete(@PathVariable @Positive Long invoiceId) {
        return invoiceService.complete(invoiceId);
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "Get invoice details, snapshots, payments, and refunds")
    public InvoiceDetailsResponse getById(@PathVariable @Positive Long invoiceId) {
        return invoiceService.getById(invoiceId);
    }

    @GetMapping
    @Operation(summary = "Search and paginate invoices")
    public PageResponse<InvoiceSummaryResponse> search(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return invoiceService.search(status, fromDate, toDate, query, page, size);
    }

    @PostMapping("/{invoiceId}/cancel")
    @Operation(summary = "Cancel an invoice", description = "Requires the Invoices and Returns permission; a reason is required")
    public InvoiceDetailsResponse cancel(
            @PathVariable @Positive Long invoiceId,
            @Valid @RequestBody CancelInvoiceRequest request) {
        return invoiceService.cancel(invoiceId, request);
    }

    @PostMapping("/{invoiceId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Record an invoice refund",
            description = "Requires the Invoices and Returns permission; supports partial/full refund status and an exact split refund payment foundation")
    public InvoiceRefundResponse createRefund(
            @PathVariable @Positive Long invoiceId,
            @Valid @RequestBody CreateInvoiceRefundRequest request) {
        return invoiceService.createRefund(invoiceId, request);
    }
}
