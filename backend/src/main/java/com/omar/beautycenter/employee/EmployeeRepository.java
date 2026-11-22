package com.omar.beautycenter.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByActiveTrueOrderByNameAscIdAsc();

    @Query("""
            SELECT employee
            FROM Employee employee
            WHERE (:active IS NULL OR employee.active = :active)
              AND (:search IS NULL
                   OR LOWER(employee.name) LIKE :search
                   OR LOWER(COALESCE(employee.phone, '')) LIKE :search
                   OR LOWER(employee.position) LIKE :search)
            """)
    Page<Employee> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}
