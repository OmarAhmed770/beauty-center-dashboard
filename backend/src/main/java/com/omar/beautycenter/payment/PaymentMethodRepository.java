package com.omar.beautycenter.payment;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByType(PaymentMethodType type);

    @Query("""
            SELECT method
            FROM PaymentMethod method
            WHERE (:active IS NULL OR method.active = :active)
              AND (:type IS NULL OR method.type = :type)
              AND (:search IS NULL
                   OR LOWER(method.code) LIKE :search
                   OR LOWER(method.displayNameArabic) LIKE :search
                   OR LOWER(method.displayNameEnglish) LIKE :search)
            """)
    Page<PaymentMethod> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("type") PaymentMethodType type,
            Pageable pageable);
}
