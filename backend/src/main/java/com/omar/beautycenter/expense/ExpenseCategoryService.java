package com.omar.beautycenter.expense;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.expense.dto.CreateExpenseCategoryRequest;
import com.omar.beautycenter.expense.dto.ExpenseCategoryResponse;
import com.omar.beautycenter.expense.dto.UpdateExpenseCategoryRequest;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;

    public ExpenseCategoryService(ExpenseCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public ExpenseCategoryResponse create(CreateExpenseCategoryRequest request) {
        String nameArabic = requiredText(request.nameArabic(), "Arabic name", 150);
        String nameEnglish = requiredText(request.nameEnglish(), "English name", 150);
        assertNamesAvailable(null, nameArabic, nameEnglish);
        ExpenseCategory category = new ExpenseCategory(
                nameArabic,
                nameEnglish,
                Boolean.TRUE.equals(request.active()),
                optionalText(request.notes(), "Notes", 2000));
        return ExpenseCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public ExpenseCategoryResponse update(Long id, UpdateExpenseCategoryRequest request) {
        ExpenseCategory category = getEntityById(id);
        String nameArabic = requiredText(request.nameArabic(), "Arabic name", 150);
        String nameEnglish = requiredText(request.nameEnglish(), "English name", 150);
        assertNamesAvailable(id, nameArabic, nameEnglish);
        category.update(
                nameArabic,
                nameEnglish,
                optionalText(request.notes(), "Notes", 2000));
        return ExpenseCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public ExpenseCategoryResponse setActive(Long id, boolean active) {
        ExpenseCategory category = getEntityById(id);
        category.setActive(active);
        return ExpenseCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('EXPENSES','SETTINGS')")
    public ExpenseCategoryResponse getById(Long id) {
        return ExpenseCategoryResponse.from(getEntityById(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('EXPENSES','SETTINGS')")
    public PageResponse<ExpenseCategoryResponse> search(
            String query,
            Boolean active,
            int page,
            int size) {
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<ExpenseCategoryResponse> result = categoryRepository.search(
                        search,
                        active,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.asc("nameEnglish"), Sort.Order.asc("id"))))
                .map(ExpenseCategoryResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ExpenseCategory getEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense category not found."));
    }

    @Transactional(readOnly = true)
    public ExpenseCategory getActiveEntityById(Long id) {
        ExpenseCategory category = getEntityById(id);
        if (!category.isActive()) {
            throw new BusinessRuleException(
                    "Inactive expense categories cannot be used for new expenses.");
        }
        return category;
    }

    private void assertNamesAvailable(Long currentId, String nameArabic, String nameEnglish) {
        boolean arabicExists = currentId == null
                ? categoryRepository.existsByNameArabicIgnoreCase(nameArabic)
                : categoryRepository.existsByNameArabicIgnoreCaseAndIdNot(nameArabic, currentId);
        boolean englishExists = currentId == null
                ? categoryRepository.existsByNameEnglishIgnoreCase(nameEnglish)
                : categoryRepository.existsByNameEnglishIgnoreCaseAndIdNot(nameEnglish, currentId);
        if (arabicExists || englishExists) {
            throw new BusinessRuleException(
                    "An expense category with one of these names already exists.");
        }
    }

    static String requiredText(String value, String field, int maximumLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must contain 1 to " + maximumLength + " characters.");
        }
        return normalized;
    }

    static String optionalText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must not exceed " + maximumLength + " characters.");
        }
        return normalized;
    }
}
