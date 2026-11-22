package com.omar.beautycenter.discount;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface DiscountRepository extends JpaRepository<Discount, Long>, JpaSpecificationExecutor<Discount> {

    default Page<Discount> search(
            Boolean active,
            DiscountType type,
            Long catalogItemId,
            Long categoryId,
            LocalDate applicableDate,
            String search,
            Pageable pageable) {
        return findAll(matching(active, type, catalogItemId, categoryId, applicableDate, search), pageable);
    }

    private static Specification<Discount> matching(
            Boolean active,
            DiscountType type,
            Long catalogItemId,
            Long categoryId,
            LocalDate applicableDate,
            String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (catalogItemId != null) {
                predicates.add(criteriaBuilder.equal(root.get("catalogItem").get("id"), catalogItemId));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (applicableDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<LocalDate>get("startDate"), applicableDate));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), applicableDate));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("name")), search));
            }
            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
