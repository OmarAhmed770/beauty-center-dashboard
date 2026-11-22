package com.omar.beautycenter.catalog;

import com.omar.beautycenter.catalog.dto.CatalogCategoryResponse;
import com.omar.beautycenter.catalog.dto.CreateCatalogCategoryRequest;
import com.omar.beautycenter.catalog.dto.UpdateCatalogCategoryRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogCategoryService {

    private final CatalogCategoryRepository categoryRepository;

    public CatalogCategoryService(CatalogCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public CatalogCategoryResponse create(CreateCatalogCategoryRequest request) {
        String nameArabic = CatalogValidation.requiredText(request.nameArabic(), "Arabic name", 150);
        String nameEnglish = CatalogValidation.requiredText(request.nameEnglish(), "English name", 150);
        assertNamesAvailable(null, nameArabic, nameEnglish);
        CatalogCategory category = new CatalogCategory(
                nameArabic,
                nameEnglish,
                Boolean.TRUE.equals(request.active()),
                CatalogValidation.optionalText(request.description(), "Description", 1000));
        return CatalogCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public CatalogCategoryResponse update(Long categoryId, UpdateCatalogCategoryRequest request) {
        CatalogCategory category = getEntityById(categoryId);
        String nameArabic = CatalogValidation.requiredText(request.nameArabic(), "Arabic name", 150);
        String nameEnglish = CatalogValidation.requiredText(request.nameEnglish(), "English name", 150);
        assertNamesAvailable(categoryId, nameArabic, nameEnglish);
        category.update(
                nameArabic,
                nameEnglish,
                CatalogValidation.optionalText(request.description(), "Description", 1000));
        return CatalogCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public CatalogCategoryResponse setActive(Long categoryId, boolean active) {
        CatalogCategory category = getEntityById(categoryId);
        category.setActive(active);
        return CatalogCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public CatalogCategoryResponse getById(Long categoryId) {
        return CatalogCategoryResponse.from(getEntityById(categoryId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public PageResponse<CatalogCategoryResponse> search(
            String query,
            Boolean active,
            int page,
            int size) {
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<CatalogCategoryResponse> result = categoryRepository.search(
                        search,
                        active,
                        PageRequest.of(page, size, Sort.by("nameEnglish").ascending()))
                .map(CatalogCategoryResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CatalogCategory getEntityById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Catalog category not found."));
    }

    @Transactional(readOnly = true)
    public CatalogCategory getActiveEntityById(Long categoryId) {
        CatalogCategory category = getEntityById(categoryId);
        if (!category.isActive()) {
            throw new BusinessRuleException("Inactive catalog categories cannot be used for new or updated records.");
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
            throw new BusinessRuleException("A catalog category with one of these names already exists.");
        }
    }
}
