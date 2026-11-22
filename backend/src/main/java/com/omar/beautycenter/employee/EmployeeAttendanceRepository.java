package com.omar.beautycenter.employee;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, Long> {

    Optional<EmployeeAttendance> findByEmployeeIdAndBusinessDate(Long employeeId, LocalDate businessDate);

    boolean existsByEmployeeIdAndBusinessDateAndStatus(
            Long employeeId,
            LocalDate businessDate,
            EmployeeAttendanceStatus status);

    @Query("""
            SELECT attendance
            FROM EmployeeAttendance attendance
            JOIN FETCH attendance.employee
            LEFT JOIN FETCH attendance.dailyWageTransaction
            WHERE attendance.businessDate = :businessDate
            ORDER BY attendance.employee.name, attendance.employee.id
            """)
    List<EmployeeAttendance> findWithDetailsByBusinessDate(@Param("businessDate") LocalDate businessDate);
}
