package com.omar.beautycenter.catalog;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface HairConfigurationRepository extends JpaRepository<HairConfiguration, Long>, JpaSpecificationExecutor<HairConfiguration> {

    default Page<HairConfiguration> search(
            Long hairItemId,
            Boolean active,
            LocalDate effectiveOn,
            String search,
            Pageable pageable) {
        return findAll(matching(hairItemId, active, effectiveOn, search), pageable);
    }

    private static Specification<HairConfiguration> matching(
            Long hairItemId,
            Boolean active,
            LocalDate effectiveOn,
            String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hairItemId != null) {
                predicates.add(criteriaBuilder.equal(root.get("hairItem").get("id"), hairItemId));
            }
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            if (effectiveOn != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<LocalDate>get("effectiveDate"), effectiveOn));
            }
            if (search != null && !search.isBlank()) {
                Join<HairConfiguration, CatalogItem> hairItem = root.join("hairItem", JoinType.INNER);
                Expression<String> color = criteriaBuilder.coalesce(root.<String>get("color"), "");
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("brand")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("length")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(color), search),
                        criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("unitType")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(hairItem.<String>get("code")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(hairItem.<String>get("nameArabic")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(hairItem.<String>get("nameEnglish")), search)));
            }
            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
