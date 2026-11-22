package com.omar.beautycenter.closing;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DailyClosingRepository extends Repository<DailyClosing, Long> {

    Optional<DailyClosing> findTopByBusinessDateOrderByClosingVersionDesc(LocalDate businessDate);

    List<DailyClosing> findByBusinessDateOrderByClosingVersionDesc(LocalDate businessDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT closing FROM DailyClosing closing
            WHERE closing.businessDate = :businessDate
              AND closing.closingVersion = (
                  SELECT MAX(latest.closingVersion) FROM DailyClosing latest
                  WHERE latest.businessDate = :businessDate)
            """)
    Optional<DailyClosing> findLatestByBusinessDateForUpdate(@Param("businessDate") LocalDate businessDate);

    <S extends DailyClosing> S saveAndFlush(S closing);

    @Query("""
            SELECT closing FROM DailyClosing closing
            WHERE (:status IS NULL OR closing.status = :status)
              AND (:fromDate IS NULL OR closing.businessDate >= :fromDate)
              AND (:toDate IS NULL OR closing.businessDate <= :toDate)
            """)
    Page<DailyClosing> search(
            @Param("status") DailyClosingStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
