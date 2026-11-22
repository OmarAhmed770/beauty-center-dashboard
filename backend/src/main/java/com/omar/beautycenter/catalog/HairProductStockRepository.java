package com.omar.beautycenter.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface HairProductStockRepository extends JpaRepository<HairProductStock, Long> {

    Optional<HairProductStock> findByHairItemId(Long hairItemId);

    List<HairProductStock> findAllByHairItemIdIn(Collection<Long> hairItemIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT stock FROM HairProductStock stock WHERE stock.hairItem.id = :hairItemId")
    Optional<HairProductStock> findByHairItemIdForUpdate(@Param("hairItemId") Long hairItemId);
}
