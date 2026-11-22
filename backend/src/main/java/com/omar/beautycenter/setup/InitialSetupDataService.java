package com.omar.beautycenter.setup;

import com.omar.beautycenter.catalog.CatalogCategory;
import com.omar.beautycenter.catalog.CatalogCategoryRepository;
import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.catalog.CatalogItemRepository;
import com.omar.beautycenter.catalog.CatalogItemType;
import com.omar.beautycenter.expense.ExpenseCategory;
import com.omar.beautycenter.expense.ExpenseCategoryRepository;
import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodRepository;
import com.omar.beautycenter.payment.PaymentMethodType;
import com.omar.beautycenter.setup.dto.StarterServiceRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.stereotype.Service;

/** Initial-only reference data creation. It never overwrites records seeded or created earlier. */
@Service
public class InitialSetupDataService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final CatalogCategoryRepository catalogCategoryRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public InitialSetupDataService(
            CatalogCategoryRepository catalogCategoryRepository,
            CatalogItemRepository catalogItemRepository,
            ExpenseCategoryRepository expenseCategoryRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public void apply(List<StarterServiceRequest> starterServices,
            boolean addDefaultExpenseCategories,
            boolean addDefaultPaymentMethods) {
        for (StarterServiceRequest starter : starterServices == null ? List.<StarterServiceRequest>of() : starterServices) {
            StarterServiceCategory category = starter.category();
            CatalogCategory catalogCategory = ensureCatalogCategory(category);
            String name = starter.nameArabic().strip();
            String code = starterCode(category, name);
            if (!catalogItemRepository.existsByCode(code)) {
                catalogItemRepository.save(new CatalogItem(
                        code,
                        name,
                        name + " — " + category.englishName(),
                        CatalogItemType.SERVICE,
                        catalogCategory,
                        ZERO,
                        ZERO,
                        false,
                        "خدمة تمهيدية — حدّد السعر قبل تفعيلها."));
            }
        }

        if (addDefaultExpenseCategories) {
            ensureExpenseCategory("الكهرباء", "Electricity", "الكهرباء", "Electricity");
            ensureExpenseCategory("المياه", "Water", "المياه", "Water");
            ensureExpenseCategory("الغسالة", "Washing machine", "المغسلة", "Laundry");
            ensureExpenseCategory("القهوة والمشروبات", "Coffee and drinks", "القهوة والضيافة", "Coffee and hospitality");
            ensureExpenseCategory("الإيجار", "Rent", "الإيجار", "Rent");
            ensureExpenseCategory("الرواتب", "Salaries", "الرواتب", "Salaries");
            ensureExpenseCategory("المواد", "Materials", "المشتريات", "Purchases");
            ensureExpenseCategory("الصيانة", "Maintenance", "الصيانة", "Maintenance");
            ensureExpenseCategory("أخرى", "Other", "أخرى", "Other");
        }

        if (addDefaultPaymentMethods) {
            ensurePaymentMethod("CASH", "نقدي", "Cash", PaymentMethodType.CASH, false, 10);
            ensurePaymentMethod("CARD", "بطاقة", "Card", PaymentMethodType.CARD, false, 20);
            ensurePaymentMethod("WALLET", "محفظة إلكترونية", "Wallet", PaymentMethodType.WALLET, false, 30);
        }
    }

    private CatalogCategory ensureCatalogCategory(StarterServiceCategory category) {
        return catalogCategoryRepository.findByNameArabicIgnoreCase(category.arabicName())
                .orElseGet(() -> catalogCategoryRepository.save(new CatalogCategory(
                        category.arabicName(), category.englishName(), true, null)));
    }

    private void ensureExpenseCategory(String arabicName, String englishName, String arabicAlias, String englishAlias) {
        boolean exists = expenseCategoryRepository.existsByNameArabicIgnoreCase(arabicName)
                || expenseCategoryRepository.existsByNameEnglishIgnoreCase(englishName)
                || expenseCategoryRepository.existsByNameArabicIgnoreCase(arabicAlias)
                || expenseCategoryRepository.existsByNameEnglishIgnoreCase(englishAlias);
        if (!exists) {
            expenseCategoryRepository.save(new ExpenseCategory(arabicName, englishName, true, null));
        }
    }

    private void ensurePaymentMethod(
            String code,
            String arabicName,
            String englishName,
            PaymentMethodType type,
            boolean requiresReference,
            int sortOrder) {
        if (!paymentMethodRepository.existsByType(type) && !paymentMethodRepository.existsByCode(code)) {
            paymentMethodRepository.save(new PaymentMethod(
                    code, arabicName, englishName, type, true, requiresReference, sortOrder));
        }
    }

    private static String starterCode(StarterServiceCategory category, String name) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((category.name() + "|" + name).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("SETUP_");
            for (int index = 0; index < 10; index++) {
                result.append(String.format("%02X", hash[index]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
