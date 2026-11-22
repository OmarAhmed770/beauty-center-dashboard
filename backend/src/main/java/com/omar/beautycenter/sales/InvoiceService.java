package com.omar.beautycenter.sales;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.catalog.HairStockService;
import com.omar.beautycenter.closing.BusinessDateWriteGuard;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodService;
import com.omar.beautycenter.sales.dto.CancelInvoiceRequest;
import com.omar.beautycenter.sales.dto.CreateDraftInvoiceRequest;
import com.omar.beautycenter.sales.dto.CreateInvoiceRefundRequest;
import com.omar.beautycenter.sales.dto.CreateQuickOrderRequest;
import com.omar.beautycenter.sales.dto.CreateSimpleOrderRequest;
import com.omar.beautycenter.sales.dto.InvoiceDetailsResponse;
import com.omar.beautycenter.sales.dto.HairRefundReturnRequest;
import com.omar.beautycenter.sales.dto.InvoicePaymentRequest;
import com.omar.beautycenter.sales.dto.InvoiceRefundResponse;
import com.omar.beautycenter.sales.dto.InvoiceSummaryResponse;
import com.omar.beautycenter.sales.dto.InvoiceTotalsPreviewResponse;
import com.omar.beautycenter.sales.dto.QuickOrderPaymentRequest;
import com.omar.beautycenter.sales.dto.QuickOrderResponse;
import com.omar.beautycenter.sales.dto.SetInvoicePaymentsRequest;
import com.omar.beautycenter.sales.dto.UpsertInvoiceItemRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final ConcurrentHashMap<String, ReentrantLock> quickOrderLocks = new ConcurrentHashMap<>();

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository itemRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final InvoiceRefundRepository refundRepository;
    private final InvoiceRefundPaymentRepository refundPaymentRepository;
    private final InvoiceItemCalculationService itemCalculationService;
    private final PaymentMethodService paymentMethodService;
    private final BusinessDateWriteGuard businessDateWriteGuard;
    private final AuditLogService auditLogService;
    private final HairStockService hairStockService;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository itemRepository,
            InvoicePaymentRepository paymentRepository,
            InvoiceRefundRepository refundRepository,
            InvoiceRefundPaymentRepository refundPaymentRepository,
            InvoiceItemCalculationService itemCalculationService,
            PaymentMethodService paymentMethodService,
            BusinessDateWriteGuard businessDateWriteGuard,
            AuditLogService auditLogService,
            HairStockService hairStockService,
            AuditorAware<String> auditorAware,
            Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.refundPaymentRepository = refundPaymentRepository;
        this.itemCalculationService = itemCalculationService;
        this.paymentMethodService = paymentMethodService;
        this.businessDateWriteGuard = businessDateWriteGuard;
        this.auditLogService = auditLogService;
        this.hairStockService = hairStockService;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse createDraft(CreateDraftInvoiceRequest request) {
        businessDateWriteGuard.assertWritable(request.businessDate());
        Invoice invoice = newDraft(
                request.businessDate(), request.customerName(), request.customerPhone(), request.notes());
        invoiceRepository.saveAndFlush(invoice);
        return details(invoice, List.of(), List.of());
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('ADD_ORDERS')")
    public InvoiceDetailsResponse createSimpleOrder(CreateSimpleOrderRequest request) {
        businessDateWriteGuard.assertWritable(request.businessDate());
        InvoiceItemSnapshot manualItem = itemCalculationService.calculateManual(
                request.description(), request.totalAmount(), request.employeeId());
        List<ResolvedPayment> resolvedPayments = resolvePayments(List.of(new InvoicePaymentRequest(
                request.paymentMethodId(), request.totalAmount(), request.paymentReference())));

        Invoice invoice = newDraft(
                request.businessDate(), request.customerName(), request.customerPhone(), request.notes());
        invoiceRepository.saveAndFlush(invoice);
        itemRepository.saveAndFlush(new InvoiceItem(invoice, manualItem));
        refreshTotals(invoice);
        replacePayments(invoice, resolvedPayments);
        invoiceRepository.saveAndFlush(invoice);

        return completeDraft(invoice);
    }

    /**
     * Creates a normal completed invoice through the established draft/item/payment lifecycle.
     * The database unique constraint is the cross-process idempotency guarantee; this transaction
     * ensures a validation failure cannot leave a draft, item, or payment behind.
     */
    @Transactional
    @PreAuthorize("@permissionAccess.has('ADD_ORDERS')")
    public QuickOrderResponse createQuickOrder(CreateQuickOrderRequest request) {
        String clientRequestId = InvoiceItemCalculationService.requiredText(
                request.clientRequestId(), "Client request id", 100);
        ReentrantLock lock = quickOrderLocks.computeIfAbsent(clientRequestId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            Invoice existing = invoiceRepository.findByClientRequestId(clientRequestId).orElse(null);
            if (existing != null) {
                assertReadableByCurrentUser(existing);
                return QuickOrderResponse.from(
                        existing,
                        itemRepository.findAllByInvoiceIdOrderById(existing.getId()));
            }

            businessDateWriteGuard.assertWritable(request.businessDate());
            List<InvoiceItemSnapshot> snapshots = request.lines().stream()
                    .map(line -> itemCalculationService.calculateQuickOrder(line, request.businessDate()))
                    .toList();
            long distinctHairProducts = snapshots.stream()
                    .filter(snapshot -> snapshot.hairConfiguration() != null)
                    .map(snapshot -> snapshot.catalogItem().getId())
                    .distinct()
                    .count();
            if (distinctHairProducts > 1) {
                throw new BusinessRuleException("Only one hair product may be used in a single order.");
            }
            Invoice invoice = newDraft(
                    request.businessDate(),
                    request.customerName(),
                    request.customerPhone(),
                    request.notes(),
                    clientRequestId);
            invoiceRepository.saveAndFlush(invoice);

            List<InvoiceItem> items = snapshots.stream()
                    .map(snapshot -> itemRepository.saveAndFlush(new InvoiceItem(invoice, snapshot)))
                    .toList();
            updateTotals(invoice, items);
            List<ResolvedPayment> payments = resolveQuickOrderPayments(request.payments(), invoice.getFinalTotal());
            replacePayments(invoice, payments);
            invoiceRepository.saveAndFlush(invoice);
            completeDraft(invoice);
            return QuickOrderResponse.from(invoice, items);
        } finally {
            lock.unlock();
            quickOrderLocks.remove(clientRequestId, lock);
        }
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse addItem(Long invoiceId, UpsertInvoiceItemRequest request) {
        Invoice invoice = getLocked(invoiceId);
        assertDraftWritable(invoice);
        InvoiceItem item = new InvoiceItem(
                invoice,
                itemCalculationService.calculate(request, invoice.getBusinessDate()));
        itemRepository.saveAndFlush(item);
        auditPriceOverride(item, invoice);
        refreshTotals(invoice);
        return details(invoice);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse updateItem(
            Long invoiceId,
            Long itemId,
            UpsertInvoiceItemRequest request) {
        Invoice invoice = getLocked(invoiceId);
        assertDraftWritable(invoice);
        InvoiceItem item = getOwnedItem(invoiceId, itemId);
        item.update(itemCalculationService.calculate(request, invoice.getBusinessDate()));
        itemRepository.saveAndFlush(item);
        auditPriceOverride(item, invoice);
        refreshTotals(invoice);
        return details(invoice);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse removeItem(Long invoiceId, Long itemId) {
        Invoice invoice = getLocked(invoiceId);
        assertDraftWritable(invoice);
        InvoiceItem item = getOwnedItem(invoiceId, itemId);
        itemRepository.delete(item);
        List<InvoiceItem> remaining = new ArrayList<>(itemRepository.findAllByInvoiceIdOrderById(invoiceId));
        remaining.removeIf(candidate -> candidate.getId().equals(itemId));
        updateTotals(invoice, remaining);
        return details(invoice, remaining, paymentRepository.findAllByInvoiceIdOrderById(invoiceId));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse setPayments(Long invoiceId, SetInvoicePaymentsRequest request) {
        Invoice invoice = getLocked(invoiceId);
        assertDraftWritable(invoice);
        List<ResolvedPayment> resolved = resolvePayments(request.payments());
        List<InvoicePayment> payments = replacePayments(invoice, resolved);
        return details(
                invoice,
                itemRepository.findAllByInvoiceIdOrderById(invoiceId),
                payments);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceTotalsPreviewResponse previewTotals(Long invoiceId) {
        Invoice invoice = getEntityById(invoiceId);
        BigDecimal paymentTotal = paymentRepository.findAllByInvoiceIdOrderById(invoiceId).stream()
                .map(InvoicePayment::getAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        return new InvoiceTotalsPreviewResponse(
                invoiceId,
                invoice.getSubtotal(),
                invoice.getDiscountTotal(),
                invoice.getFinalTotal(),
                paymentTotal,
                invoice.getFinalTotal().subtract(paymentTotal)
                        .setScale(2, RoundingMode.UNNECESSARY));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse complete(Long invoiceId) {
        return completeDraft(getLocked(invoiceId));
    }

    private InvoiceDetailsResponse completeDraft(Invoice invoice) {
        assertDraftWritable(invoice);
        List<InvoiceItem> items = itemRepository.findAllByInvoiceIdOrderById(invoice.getId());
        if (items.isEmpty()) {
            throw new BusinessRuleException("An invoice must contain at least one item.");
        }
        for (InvoiceItem item : items) {
            itemCalculationService.validateSnapshotForCompletion(item, invoice.getBusinessDate());
        }
        updateTotals(invoice, items);

        List<InvoicePayment> payments = paymentRepository.findAllByInvoiceIdOrderById(invoice.getId());
        BigDecimal paymentTotal = ZERO;
        for (InvoicePayment payment : payments) {
            PaymentMethod activeMethod = paymentMethodService.getActiveEntityById(
                    payment.getPaymentMethod().getId());
            validateReference(activeMethod, payment.getReferenceNumber());
            paymentTotal = paymentTotal.add(payment.getAmount());
        }
        paymentTotal = paymentTotal.setScale(2, RoundingMode.UNNECESSARY);
        if (paymentTotal.compareTo(invoice.getFinalTotal()) != 0) {
            throw new BusinessRuleException("Payment total must equal the final invoice amount.");
        }

        String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
        invoice.complete(actor, LocalDateTime.now(clock));
        invoiceRepository.saveAndFlush(invoice);
        hairStockService.deductCompletedInvoiceHair(invoice, items);
        auditLogService.record(
                "INVOICE_COMPLETED",
                "INVOICE",
                invoice.getId(),
                invoice.getBusinessDate(),
                Map.of(
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "itemCount", items.size(),
                        "paymentCount", payments.size(),
                        "finalTotal", invoice.getFinalTotal()));
        return details(invoice, items, payments);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('ADD_ORDERS','INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse getById(Long invoiceId) {
        return details(assertReadableByCurrentUser(getEntityById(invoiceId)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('ADD_ORDERS','INVOICES_AND_RETURNS')")
    public PageResponse<InvoiceSummaryResponse> search(
            InvoiceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String query,
            int page,
            int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("fromDate must be on or before toDate.");
        }
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<InvoiceSummaryResponse> result = invoiceRepository.search(
                        status,
                        receptionistUsernameOrNull(),
                        fromDate,
                        toDate,
                        search,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.desc("businessDate"),
                                        Sort.Order.desc("id"))))
                .map(InvoiceSummaryResponse::from);
        return PageResponse.from(result);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceDetailsResponse cancel(Long invoiceId, CancelInvoiceRequest request) {
        Invoice invoice = getLocked(invoiceId);
        businessDateWriteGuard.assertWritable(invoice.getBusinessDate());
        InvoiceStatus previousStatus = invoice.getStatus();
        String reason = InvoiceItemCalculationService.requiredText(
                request.reason(), "Cancellation reason", 500);
        invoice.cancel(
                auditorAware.getCurrentAuditor().orElse("SYSTEM"),
                LocalDateTime.now(clock),
                reason);
        invoiceRepository.saveAndFlush(invoice);
        if (previousStatus == InvoiceStatus.COMPLETED) {
            hairStockService.restoreCancelledInvoiceHair(
                    invoice,
                    itemRepository.findAllByInvoiceIdOrderById(invoice.getId()),
                    reason);
        }
        auditLogService.record(
                "INVOICE_CANCELLED",
                "INVOICE",
                invoice.getId(),
                invoice.getBusinessDate(),
                Map.of(
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "previousStatus", previousStatus,
                        "reason", reason,
                        "finalTotal", invoice.getFinalTotal()));
        return details(invoice);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('INVOICES_AND_RETURNS')")
    public InvoiceRefundResponse createRefund(
            Long invoiceId,
            CreateInvoiceRefundRequest request) {
        Invoice invoice = getLocked(invoiceId);
        businessDateWriteGuard.assertWritable(request.businessDate());
        if (request.businessDate().isBefore(invoice.getBusinessDate())) {
            throw new BusinessRuleException(
                    "Refund business date cannot be before the invoice business date.");
        }
        BigDecimal amount = InvoiceItemCalculationService.positiveMoney(
                request.amount(), "Refund amount");
        String reason = InvoiceItemCalculationService.requiredText(
                request.reason(), "Refund reason", 500);
        List<ResolvedPayment> resolvedPayments = resolvePayments(request.payments());
        BigDecimal paymentTotal = resolvedPayments.stream()
                .map(ResolvedPayment::amount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        if (paymentTotal.compareTo(amount) != 0) {
            throw new BusinessRuleException("Refund payment total must equal the refund amount.");
        }
        Map<InvoiceItem, BigDecimal> returnedHair = resolveHairRefundReturns(invoice, request.hairReturns());

        invoice.registerRefund(amount);
        InvoiceRefund refund = refundRepository.saveAndFlush(
                new InvoiceRefund(invoice, request.businessDate(), amount, reason));
        List<InvoiceRefundPayment> refundPayments = resolvedPayments.stream()
                .map(payment -> new InvoiceRefundPayment(
                        refund,
                        payment.method(),
                        payment.amount(),
                        payment.referenceNumber()))
                .toList();
        refundPaymentRepository.saveAll(refundPayments);
        invoiceRepository.saveAndFlush(invoice);
        hairStockService.restoreRefundedHair(refund, returnedHair, reason);
        auditLogService.record(
                "INVOICE_REFUNDED",
                "INVOICE",
                invoice.getId(),
                request.businessDate(),
                Map.of(
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "refundId", refund.getId(),
                        "refundAmount", amount,
                        "refundedTotal", invoice.getRefundedTotal(),
                        "invoiceStatus", invoice.getStatus()));
        return InvoiceRefundResponse.from(refund, refundPayments);
    }

    private List<ResolvedPayment> resolvePayments(List<InvoicePaymentRequest> requests) {
        if (requests == null) {
            throw new BusinessRuleException("Payments are required.");
        }
        Set<Long> methodIds = new HashSet<>();
        List<ResolvedPayment> resolved = new ArrayList<>();
        for (InvoicePaymentRequest request : requests) {
            if (request == null) {
                throw new BusinessRuleException("Payment entries cannot be null.");
            }
            if (!methodIds.add(request.paymentMethodId())) {
                throw new BusinessRuleException(
                        "A payment method may appear only once in a payment split.");
            }
            PaymentMethod method = paymentMethodService.getActiveEntityById(
                    request.paymentMethodId());
            String reference = InvoiceItemCalculationService.optionalText(
                    request.referenceNumber(), "Reference number", 200);
            validateReference(method, reference);
            resolved.add(new ResolvedPayment(
                    method,
                    InvoiceItemCalculationService.positiveMoney(
                            request.amount(), "Payment amount"),
                    reference));
        }
        return resolved;
    }

    private Map<InvoiceItem, BigDecimal> resolveHairRefundReturns(
            Invoice invoice,
            List<HairRefundReturnRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }
        Map<InvoiceItem, BigDecimal> result = new LinkedHashMap<>();
        for (HairRefundReturnRequest request : requests) {
            if (request == null) {
                throw new BusinessRuleException("Returned hair entries cannot be null.");
            }
            InvoiceItem item = getOwnedItem(invoice.getId(), request.invoiceItemId());
            if (item.getHairConfiguration() == null) {
                throw new BusinessRuleException("Only hair invoice items can return grams to stock.");
            }
            BigDecimal grams = request.grams();
            if (grams == null || grams.signum() <= 0) {
                throw new BusinessRuleException("Returned hair quantity must be greater than zero.");
            }
            try {
                grams = grams.setScale(3, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException exception) {
                throw new BusinessRuleException("Returned hair quantity must have at most 3 decimal digits.");
            }
            if (result.put(item, grams) != null) {
                throw new BusinessRuleException("A hair invoice item may appear only once in a refund.");
            }
        }
        return result;
    }

    private List<ResolvedPayment> resolveQuickOrderPayments(
            List<QuickOrderPaymentRequest> requests,
            BigDecimal invoiceTotal) {
        if (requests == null || requests.isEmpty()) {
            PaymentMethod cash = paymentMethodService.getActiveEntityByCode("CASH");
            return resolvePayments(List.of(new InvoicePaymentRequest(cash.getId(), invoiceTotal, null)));
        }
        if (requests.size() == 1 && requests.get(0).amount() == null) {
            QuickOrderPaymentRequest payment = requests.get(0);
            return resolvePayments(List.of(new InvoicePaymentRequest(
                    payment.paymentMethodId(), invoiceTotal, payment.referenceNumber())));
        }
        if (requests.stream().anyMatch(payment -> payment.amount() == null)) {
            throw new BusinessRuleException(
                    "Each payment amount is required when more than one payment is supplied.");
        }
        return resolvePayments(requests.stream()
                .map(payment -> new InvoicePaymentRequest(
                        payment.paymentMethodId(), payment.amount(), payment.referenceNumber()))
                .toList());
    }

    private static void validateReference(PaymentMethod method, String reference) {
        if (method.isRequiresReferenceNumber() && reference == null) {
            throw new BusinessRuleException(
                    "A reference number is required for the selected payment method.");
        }
    }

    private void assertDraftWritable(Invoice invoice) {
        invoice.assertDraft();
        businessDateWriteGuard.assertWritable(invoice.getBusinessDate());
    }

    private void refreshTotals(Invoice invoice) {
        updateTotals(invoice, itemRepository.findAllByInvoiceIdOrderById(invoice.getId()));
    }

    private List<InvoicePayment> replacePayments(Invoice invoice, List<ResolvedPayment> resolved) {
        paymentRepository.deleteAllByInvoiceId(invoice.getId());
        List<InvoicePayment> payments = resolved.stream()
                .map(payment -> new InvoicePayment(
                        invoice,
                        payment.method(),
                        payment.amount(),
                        payment.referenceNumber()))
                .toList();
        paymentRepository.saveAll(payments);
        return payments;
    }

    private Invoice newDraft(
            LocalDate businessDate,
            String customerName,
            String customerPhone,
            String notes) {
        return newDraft(businessDate, customerName, customerPhone, notes, null);
    }

    private Invoice newDraft(
            LocalDate businessDate,
            String customerName,
            String customerPhone,
            String notes,
            String clientRequestId) {
        return new Invoice(
                nextInvoiceNumber(businessDate),
                businessDate,
                InvoiceItemCalculationService.optionalText(customerName, "Customer name", 150),
                InvoiceItemCalculationService.optionalText(customerPhone, "Customer phone", 50),
                InvoiceItemCalculationService.optionalText(notes, "Notes", 2000),
                clientRequestId);
    }

    private void updateTotals(Invoice invoice, List<InvoiceItem> items) {
        BigDecimal subtotal = items.stream()
                .map(InvoiceItem::getLineSubtotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal discountTotal = items.stream()
                .map(InvoiceItem::getDiscountAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal finalTotal = items.stream()
                .map(InvoiceItem::getFinalLineTotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        invoice.updateTotals(subtotal, discountTotal, finalTotal);
        invoiceRepository.saveAndFlush(invoice);
    }

    private InvoiceItem getOwnedItem(Long invoiceId, Long itemId) {
        InvoiceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Invoice item not found."));
        if (!item.getInvoice().getId().equals(invoiceId)) {
            throw new NotFoundException("Invoice item not found.");
        }
        return item;
    }

    private Invoice getLocked(Long invoiceId) {
        return invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found."));
    }

    private Invoice getEntityById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found."));
    }

    private Invoice assertReadableByCurrentUser(Invoice invoice) {
        String receptionistUsername = receptionistUsernameOrNull();
        if (receptionistUsername != null && !receptionistUsername.equals(invoice.getCreatedBy())) {
            throw new NotFoundException("Invoice not found.");
        }
        return invoice;
    }

    private static String receptionistUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        boolean canReadAllInvoices = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_INVOICES_AND_RETURNS"));
        return canReadAllInvoices ? null : authentication.getName();
    }

    private InvoiceDetailsResponse details(Invoice invoice) {
        return details(
                invoice,
                itemRepository.findAllByInvoiceIdOrderById(invoice.getId()),
                paymentRepository.findAllByInvoiceIdOrderById(invoice.getId()));
    }

    private InvoiceDetailsResponse details(
            Invoice invoice,
            List<InvoiceItem> items,
            List<InvoicePayment> payments) {
        List<InvoiceRefundResponse> refunds = refundRepository
                .findAllByInvoiceIdOrderById(invoice.getId())
                .stream()
                .map(refund -> InvoiceRefundResponse.from(
                        refund,
                        refundPaymentRepository.findAllByRefundIdOrderById(refund.getId())))
                .toList();
        return InvoiceDetailsResponse.from(invoice, items, payments, refunds, canViewCosts());
    }

    private static String nextInvoiceNumber(LocalDate businessDate) {
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
        return "INV-" + businessDate.format(NUMBER_DATE) + "-" + random;
    }

    private static boolean canViewCosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_SERVICES_AND_PRICES"));
    }

    private void auditPriceOverride(InvoiceItem item, Invoice invoice) {
        if (item.getPriceOverrideAmount() != null) {
            auditLogService.record("PRICE_OVERRIDE", "INVOICE_ITEM", item.getId(),
                    invoice.getBusinessDate(), Map.of(
                            "invoiceId", invoice.getId(),
                            "originalAmount", item.getPriceOverrideOriginalAmount(),
                            "overrideAmount", item.getPriceOverrideAmount(),
                            "reason", item.getPriceOverrideReason()));
        }
    }

    private record ResolvedPayment(
            PaymentMethod method,
            BigDecimal amount,
            String referenceNumber) {
    }
}
