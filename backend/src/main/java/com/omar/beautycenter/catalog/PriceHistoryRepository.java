package com.omar.beautycenter.catalog;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long>, JpaSpecificationExecutor<PriceHistory> {

    /**
     * Builds predicates only for supplied filters. PostgreSQL cannot infer the type of
     * a nullable bind used only in a JPQL {@code :value is null} predicate, unlike H2.
     */
    default Page<PriceHistory> search(
            Long catalogItemId,
            PriceValueType valueType,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime,
            Pageable pageable) {
        return findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (catalogItemId != null) {
                predicates.add(criteriaBuilder.equal(root.get("catalogItem").get("id"), catalogItemId));
            }
            if (valueType != null) {
                predicates.add(criteriaBuilder.equal(root.get("valueType"), valueType));
            }
            if (fromDateTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("effectiveAt"), fromDateTime));
            }
            if (toDateTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.<LocalDateTime>get("effectiveAt"), toDateTime));
            }
            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }, pageable);
    }
}
